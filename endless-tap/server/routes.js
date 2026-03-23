// server/routes.js
// All REST API endpoints

const express = require('express');
const router  = express.Router();
const db      = require('./db');
const gc      = require('./game-config');
const { requireAuth } = require('./auth');

// Apply auth to all routes below
router.use(requireAuth);

// ─────────────────────────────────────────────────
//  POST /api/login
//  Called on every app open. Returns full user state.
//  Also processes referral if ?ref=TELEGRAM_ID in query
// ─────────────────────────────────────────────────
router.post('/login', (req, res) => {
  const tgUser = req.tgUser;
  const refId  = parseInt(req.query.ref) || null;

  // Create or load user
  let user = db.getUser(tgUser.id);
  const isNew = !user;

  if (isNew) {
    db.createUser(tgUser, refId);
    user = db.getUser(tgUser.id);

    // Process referral bonuses
    if (refId && refId !== tgUser.id) {
      const referrer = db.getUser(refId);
      if (referrer) {
        db.processReferral(refId, tgUser.id, gc.REFERRAL_BONUS.referrer, gc.REFERRAL_BONUS.referred);
      }
    }
  }

  // Update last seen + name (in case it changed)
  db.touchUser(tgUser.id, {
    username:   tgUser.username   || user.username,
    first_name: tgUser.first_name || user.first_name,
  });

  // Reset boosts if new day
  const freshBoosts = db.resetBoostsIfNeeded(user);
  user = db.getUser(tgUser.id); // re-fetch after potential update

  // Passive income: add gas earned while offline
  const passiveRateHz = user.passive_rate / 3600; // per second
  if (passiveRateHz > 0 && user.last_seen_at) {
    const lastSeen = new Date(user.last_seen_at);
    const now      = new Date();
    const seconds  = Math.min((now - lastSeen) / 1000, 8 * 3600); // cap at 8h
    const earned   = Math.floor(passiveRateHz * seconds);
    if (earned > 0) {
      db.recordTaps(tgUser.id, 0, earned);
      user.gas += earned;
    }
  }

  const upgrades = db.getUserUpgrades(tgUser.id);
  const tasks    = db.getUserTasks(tgUser.id);
  const rank     = db.getUserRank(tgUser.id);
  const stats    = db.getGlobalStats();
  const level    = gc.getLevelForGas(user.total_gas_earned);
  const cipher   = gc.getTodayCipher();

  res.json({
    ok: true,
    user: {
      id:             user.telegram_id,
      username:       user.username,
      first_name:     user.first_name,
      gas:            user.gas,
      total_taps:     user.total_taps,
      level:          level,
      energy:         Math.min(user.energy, user.max_energy),
      max_energy:     user.max_energy,
      tap_power:      gc.computeTapPower(user, upgrades),
      passive_rate:   user.passive_rate,
      boosts_left:    freshBoosts,
      referral_count: user.referral_count,
      rank:           rank,
      cipher_solved_today: db.hasSolvedCipherToday(user),
      is_new:         isNew,
    },
    upgrades,
    tasks: [...tasks],
    global: stats,
    cipher: {
      hint: cipher.hint,
      reward: Math.floor(gc.CIPHER_BASE_REWARD * (1 + (upgrades['cipher'] || 0) * 0.5)),
    },
  });
});

// ─────────────────────────────────────────────────
//  POST /api/tap
//  Body: { taps: number, energy_used: number }
//  Server validates and records taps
// ─────────────────────────────────────────────────
router.post('/tap', (req, res) => {
  const id   = req.tgUser.id;
  const user = db.getUser(id);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const taps      = Math.max(1, Math.min(parseInt(req.body.taps) || 1, 50));
  const upgrades  = db.getUserUpgrades(id);
  const level     = gc.getLevelForGas(user.total_gas_earned);
  const tapPower  = gc.computeTapPower(user, upgrades) + level.tapBonus;

  // Rate limit check
  const rl = db.checkTapRateLimit(id, taps);
  if (!rl.ok) return res.status(429).json({ error: rl.reason });

  // Energy check (server-side)
  if (user.energy < taps) {
    return res.status(400).json({ error: 'Not enough energy', energy: user.energy });
  }

  const gasEarned = taps * tapPower;

  // Record taps
  db.recordTaps(id, taps, gasEarned);

  // Deduct energy
  db.touchUser(id, { energy: Math.max(0, user.energy - taps) });

  // Re-fetch for fresh state
  const fresh = db.getUser(id);
  const freshLevel = gc.getLevelForGas(fresh.total_gas_earned);

  res.json({
    ok: true,
    gas:       fresh.gas,
    energy:    fresh.energy,
    total_taps:fresh.total_taps,
    gas_earned: gasEarned,
    level:     freshLevel,
  });
});

// ─────────────────────────────────────────────────
//  POST /api/upgrade
//  Body: { upgrade_id: string }
// ─────────────────────────────────────────────────
router.post('/upgrade', (req, res) => {
  const id        = req.tgUser.id;
  const upgradeId = req.body.upgrade_id;
  const upg       = gc.UPGRADES.find(u => u.id === upgradeId);
  if (!upg) return res.status(400).json({ error: 'Unknown upgrade' });

  const user     = db.getUser(id);
  const upgrades = db.getUserUpgrades(id);
  const curLevel = upgrades[upgradeId] || 0;

  if (curLevel >= upg.maxLevel) {
    return res.status(400).json({ error: 'Already at max level' });
  }

  const cost = gc.getUpgradeCost(upgradeId, curLevel);
  if (user.gas < cost) {
    return res.status(400).json({ error: 'Not enough GAS', need: cost, have: user.gas });
  }

  const newLevel = curLevel + 1;
  db.deductGas(id, cost);
  db.setUpgradeLevel(id, upgradeId, newLevel);

  // Recalculate derived stats and update user row
  const allUpgrades = db.getUserUpgrades(id);
  const newPassive  = gc.computePassiveRate(allUpgrades);
  const newEnergy   = gc.computeMaxEnergy(allUpgrades);
  const newRegen    = gc.computeEnergyRegen(allUpgrades);
  const newTap      = gc.computeTapPower(user, allUpgrades);

  db.touchUser(id, {
    passive_rate: newPassive,
    max_energy:   newEnergy,
    energy_regen: newRegen,
    tap_power:    newTap,
  });

  const fresh = db.getUser(id);
  res.json({
    ok: true,
    upgrade_id:   upgradeId,
    new_level:    newLevel,
    gas:          fresh.gas,
    passive_rate: newPassive,
    max_energy:   newEnergy,
    tap_power:    newTap,
  });
});

// ─────────────────────────────────────────────────
//  POST /api/boost
//  Activates an energy boost
// ─────────────────────────────────────────────────
router.post('/boost', (req, res) => {
  const id   = req.tgUser.id;
  const user = db.getUser(id);
  if (!user) return res.status(404).json({ error: 'User not found' });

  const boosts = db.resetBoostsIfNeeded(user);
  if (boosts <= 0) {
    return res.status(400).json({ error: 'No boosts left today' });
  }

  db.useBoost(id);
  const fresh = db.getUser(id);

  res.json({
    ok:          true,
    boosts_left: fresh.boosts_left,
    energy:      fresh.energy,
    max_energy:  fresh.max_energy,
  });
});

// ─────────────────────────────────────────────────
//  POST /api/cipher
//  Body: { answer: ['OG','ETH','NFT'] }
// ─────────────────────────────────────────────────
router.post('/cipher', (req, res) => {
  const id   = req.tgUser.id;
  const user = db.getUser(id);

  if (db.hasSolvedCipherToday(user)) {
    return res.status(400).json({ error: 'Already solved today' });
  }

  const today  = new Date().toISOString().slice(0,10);
  const cipher = gc.getTodayCipher();
  const answer = req.body.answer;

  if (!Array.isArray(answer) || answer.length !== cipher.answer.length) {
    return res.status(400).json({ error: 'Invalid answer format' });
  }

  const correct = cipher.answer.every((a, i) => a === answer[i]);
  if (!correct) {
    return res.status(400).json({ error: 'Wrong answer', hint: cipher.hint });
  }

  const upgrades = db.getUserUpgrades(id);
  const reward   = Math.floor(
    gc.CIPHER_BASE_REWARD * (1 + (upgrades['cipher'] || 0) * 0.5)
  );

  db.markCipherSolved(id, today);
  db.recordTaps(id, 0, reward);

  res.json({
    ok:     true,
    reward: reward,
    gas:    db.getUser(id).gas,
  });
});

// ─────────────────────────────────────────────────
//  POST /api/task
//  Body: { task_id: string }
// ─────────────────────────────────────────────────
router.post('/task', (req, res) => {
  const id     = req.tgUser.id;
  const taskId = req.body.task_id;
  const task   = gc.TASKS.find(t => t.id === taskId);
  if (!task) return res.status(400).json({ error: 'Unknown task' });

  const user       = db.getUser(id);
  const completed  = db.getUserTasks(id);

  // Handle daily tasks (allow once per day)
  if (task.daily) {
    const todayKey = taskId + '_' + new Date().toISOString().slice(0,10);
    if (completed.has(todayKey)) {
      return res.status(400).json({ error: 'Already done today' });
    }
    const ok = db.completeTask(id, todayKey, task.reward);
    return res.json({ ok, reward: task.reward, gas: db.getUser(id).gas });
  }

  if (completed.has(taskId)) {
    return res.status(400).json({ error: 'Already completed' });
  }

  // Validate condition
  let condMet = false;
  if (task.cond === 'manual')         condMet = true;
  else if (task.cond === 'anyUpgrade') {
    const upgrades = db.getUserUpgrades(id);
    condMet = Object.values(upgrades).some(v => v > 0);
  }
  else if (task.cond?.startsWith('taps>=')) condMet = user.total_taps >= parseInt(task.cond.split('>=')[1]);
  else if (task.cond?.startsWith('refs>=')) condMet = user.referral_count >= parseInt(task.cond.split('>=')[1]);
  else if (task.cond?.startsWith('level>=')) {
    const lv = gc.getLevelForGas(user.total_gas_earned);
    condMet = lv.level >= parseInt(task.cond.split('>=')[1]);
  }

  if (!condMet) {
    return res.status(400).json({ error: 'Task condition not met yet' });
  }

  const ok = db.completeTask(id, taskId, task.reward);
  res.json({ ok, reward: task.reward, gas: db.getUser(id).gas });
});

// ─────────────────────────────────────────────────
//  GET /api/leaderboard
// ─────────────────────────────────────────────────
router.get('/leaderboard', (req, res) => {
  const board = db.getLeaderboard(100);
  const myId  = req.tgUser.id;
  const rank  = db.getUserRank(myId);

  const result = board.map((u, i) => ({
    rank:       i + 1,
    telegram_id:u.telegram_id,
    name:       u.first_name || u.username || 'Anonymous',
    username:   u.username,
    gas:        u.gas,
    level:      gc.getLevelForGas(u.gas).name,
    total_taps: u.total_taps,
    is_me:      u.telegram_id === myId,
  }));

  res.json({ ok: true, leaderboard: result, my_rank: rank });
});

// ─────────────────────────────────────────────────
//  GET /api/referrals
// ─────────────────────────────────────────────────
router.get('/referrals', (req, res) => {
  const id   = req.tgUser.id;
  const refs = db.getReferrals(id);
  const user = db.getUser(id);
  res.json({
    ok: true,
    referral_count:       user.referral_count,
    referral_bonus_earned:user.referral_bonus_earned,
    referrals: refs.map(r => ({
      name:     r.first_name || r.username || 'User',
      username: r.username,
      gas:      r.gas,
      joined:   r.created_at,
    })),
  });
});

// ─────────────────────────────────────────────────
//  GET /api/stats  (global stats for homepage hype)
// ─────────────────────────────────────────────────
router.get('/stats', (req, res) => {
  res.json({ ok: true, ...db.getGlobalStats() });
});

module.exports = router;

// server/db.js
// All database operations. Clean separation from route logic.

const Database = require('better-sqlite3');
const path     = require('path');
const fs       = require('fs');

let db;

function getDB() {
  if (db) return db;
  const dbPath = '/tmp/endless.db';
  const dir    = path.dirname(dbPath);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  return db;
}

// ─────────────────────────────────────────────────
//  USER
// ─────────────────────────────────────────────────

function getUser(telegramId) {
  return getDB().prepare(`SELECT * FROM users WHERE telegram_id = ?`).get(telegramId);
}

function createUser(tgUser, referredBy = null) {
  const db = getDB();
  db.prepare(`
    INSERT OR IGNORE INTO users
      (telegram_id, username, first_name, last_name, referred_by)
    VALUES (?, ?, ?, ?, ?)
  `).run(
    tgUser.id,
    tgUser.username || null,
    tgUser.first_name || null,
    tgUser.last_name  || null,
    referredBy        || null
  );
  return getUser(tgUser.id);
}

function touchUser(telegramId, updates = {}) {
  const db    = getDB();
  const sets  = Object.keys(updates).map(k => `${k} = @${k}`).join(', ');
  if (!sets) {
    db.prepare(`UPDATE users SET last_seen_at = datetime('now') WHERE telegram_id = @id`)
      .run({ id: telegramId });
    return;
  }
  db.prepare(`
    UPDATE users SET ${sets}, last_seen_at = datetime('now')
    WHERE telegram_id = @id
  `).run({ ...updates, id: telegramId });
}

// ─────────────────────────────────────────────────
//  TAPS  (server-authoritative, rate-limited)
// ─────────────────────────────────────────────────

function recordTaps(telegramId, tapCount, gasEarned) {
  const db = getDB();
  db.prepare(`
    UPDATE users SET
      gas             = gas + ?,
      total_gas_earned= total_gas_earned + ?,
      total_taps      = total_taps + ?,
      last_tap_at     = datetime('now'),
      last_seen_at    = datetime('now')
    WHERE telegram_id = ?
  `).run(gasEarned, gasEarned, tapCount, telegramId);
}

// Anti-cheat: track taps per minute
function checkTapRateLimit(telegramId, tapCount) {
  const db  = getDB();
  const now = new Date().toISOString().slice(0,16); // minute precision

  const row = db.prepare(
    `SELECT * FROM tap_sessions WHERE telegram_id = ?`
  ).get(telegramId);

  const MAX_TAPS_PER_MINUTE = 600; // ~10/sec, generous for multi-touch

  if (!row) {
    db.prepare(`INSERT INTO tap_sessions (telegram_id, taps_this_minute, minute_start) VALUES (?,?,?)`)
      .run(telegramId, tapCount, now);
    return { ok: true, taps: tapCount };
  }

  if (row.minute_start !== now) {
    // New minute — reset
    db.prepare(`UPDATE tap_sessions SET taps_this_minute=?, minute_start=? WHERE telegram_id=?`)
      .run(tapCount, now, telegramId);
    return { ok: true, taps: tapCount };
  }

  const total = row.taps_this_minute + tapCount;
  if (total > MAX_TAPS_PER_MINUTE) {
    return { ok: false, reason: 'Rate limit exceeded' };
  }

  db.prepare(`UPDATE tap_sessions SET taps_this_minute=? WHERE telegram_id=?`)
    .run(total, telegramId);
  return { ok: true, taps: tapCount };
}

// ─────────────────────────────────────────────────
//  UPGRADES
// ─────────────────────────────────────────────────

function getUserUpgrades(telegramId) {
  const rows = getDB().prepare(
    `SELECT upgrade_id, level FROM upgrades WHERE telegram_id = ?`
  ).all(telegramId);
  const map = {};
  rows.forEach(r => map[r.upgrade_id] = r.level);
  return map;
}

function setUpgradeLevel(telegramId, upgradeId, newLevel) {
  getDB().prepare(`
    INSERT INTO upgrades (telegram_id, upgrade_id, level)
    VALUES (?, ?, ?)
    ON CONFLICT(telegram_id, upgrade_id) DO UPDATE SET level = excluded.level, bought_at = datetime('now')
  `).run(telegramId, upgradeId, newLevel);
}

function deductGas(telegramId, amount) {
  getDB().prepare(`
    UPDATE users SET gas = gas - ? WHERE telegram_id = ? AND gas >= ?
  `).run(amount, telegramId, amount);
}

// ─────────────────────────────────────────────────
//  TASKS
// ─────────────────────────────────────────────────

function getUserTasks(telegramId) {
  const rows = getDB().prepare(
    `SELECT task_id FROM tasks WHERE telegram_id = ?`
  ).all(telegramId);
  return new Set(rows.map(r => r.task_id));
}

function completeTask(telegramId, taskId, gasReward) {
  const db = getDB();
  try {
    db.prepare(`
      INSERT INTO tasks (telegram_id, task_id, gas_earned) VALUES (?,?,?)
    `).run(telegramId, taskId, gasReward);
    db.prepare(`UPDATE users SET gas = gas + ?, total_gas_earned = total_gas_earned + ? WHERE telegram_id = ?`)
      .run(gasReward, gasReward, telegramId);
    return true;
  } catch (e) {
    return false; // Already completed (UNIQUE constraint)
  }
}

// ─────────────────────────────────────────────────
//  REFERRALS
// ─────────────────────────────────────────────────

function processReferral(referrerId, newUserId, referrerBonus, newUserBonus) {
  const db = getDB();
  const tx = db.transaction(() => {
    try {
      db.prepare(`INSERT INTO referrals (referrer_id, referred_id, bonus_paid) VALUES (?,?,?)`)
        .run(referrerId, newUserId, referrerBonus);
      db.prepare(`UPDATE users SET gas=gas+?, total_gas_earned=total_gas_earned+?, referral_count=referral_count+?, referral_bonus_earned=referral_bonus_earned+? WHERE telegram_id=?`)
        .run(referrerBonus, referrerBonus, 1, referrerBonus, referrerId);
      db.prepare(`UPDATE users SET gas=gas+?, total_gas_earned=total_gas_earned+? WHERE telegram_id=?`)
        .run(newUserBonus, newUserBonus, newUserId);
    } catch (e) {
      // Referral already processed
    }
  });
  tx();
}

function getReferrals(telegramId) {
  return getDB().prepare(`
    SELECT u.telegram_id, u.username, u.first_name, u.gas, r.created_at
    FROM referrals r
    JOIN users u ON u.telegram_id = r.referred_id
    WHERE r.referrer_id = ?
    ORDER BY r.created_at DESC
    LIMIT 50
  `).all(telegramId);
}

// ─────────────────────────────────────────────────
//  LEADERBOARD
// ─────────────────────────────────────────────────

function getLeaderboard(limit = 100) {
  return getDB().prepare(`
    SELECT telegram_id, username, first_name, gas, total_taps, level,
           referral_count, created_at
    FROM users
    ORDER BY gas DESC
    LIMIT ?
  `).all(limit);
}

function getUserRank(telegramId) {
  const row = getDB().prepare(`
    SELECT COUNT(*) + 1 AS rank
    FROM users
    WHERE gas > (SELECT gas FROM users WHERE telegram_id = ?)
  `).get(telegramId);
  return row ? row.rank : null;
}

// ─────────────────────────────────────────────────
//  CIPHER
// ─────────────────────────────────────────────────

function markCipherSolved(telegramId, dateStr) {
  getDB().prepare(`
    UPDATE users SET cipher_solved_at=datetime('now'), cipher_date=?
    WHERE telegram_id=?
  `).run(dateStr, telegramId);
}

function hasSolvedCipherToday(user) {
  const today = new Date().toISOString().slice(0,10);
  return user.cipher_date === today;
}

// ─────────────────────────────────────────────────
//  BOOSTS
// ─────────────────────────────────────────────────

function resetBoostsIfNeeded(user) {
  const today = new Date().toISOString().slice(0,10);
  if (user.boosts_reset_at !== today) {
    getDB().prepare(`
      UPDATE users SET boosts_left=3, boosts_reset_at=? WHERE telegram_id=?
    `).run(today, user.telegram_id);
    return 3;
  }
  return user.boosts_left;
}

function useBoost(telegramId) {
  getDB().prepare(`
    UPDATE users SET boosts_left = boosts_left - 1, energy = max_energy
    WHERE telegram_id = ? AND boosts_left > 0
  `).run(telegramId);
}

// ─────────────────────────────────────────────────
//  STATS
// ─────────────────────────────────────────────────

function getGlobalStats() {
  const db = getDB();
  return {
    total_players: db.prepare(`SELECT COUNT(*) as c FROM users`).get().c,
    total_gas:     db.prepare(`SELECT SUM(total_gas_earned) as s FROM users`).get().s || 0,
    total_taps:    db.prepare(`SELECT SUM(total_taps) as s FROM users`).get().s || 0,
    online_1h:     db.prepare(`SELECT COUNT(*) as c FROM users WHERE last_seen_at > datetime('now','-1 hour')`).get().c,
  };
}

module.exports = {
  getDB, getUser, createUser, touchUser,
  recordTaps, checkTapRateLimit,
  getUserUpgrades, setUpgradeLevel, deductGas,
  getUserTasks, completeTask,
  processReferral, getReferrals,
  getLeaderboard, getUserRank,
  markCipherSolved, hasSolvedCipherToday,
  resetBoostsIfNeeded, useBoost,
  getGlobalStats,
};

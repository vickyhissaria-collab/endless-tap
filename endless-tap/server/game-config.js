// server/game-config.js
// Single source of truth for all game balance

const LEVELS = [
  { level:1, name:'🌱 Rookie',      minGas:0,         tapBonus:0,  maxEnergy:1000 },
  { level:2, name:'🌿 Learner',     minGas:5000,       tapBonus:1,  maxEnergy:1500 },
  { level:3, name:'💎 Domainer',    minGas:25000,      tapBonus:2,  maxEnergy:2000 },
  { level:4, name:'🔥 Speculator',  minGas:100000,     tapBonus:3,  maxEnergy:2500 },
  { level:5, name:'⚡ Trader',      minGas:500000,     tapBonus:5,  maxEnergy:3000 },
  { level:6, name:'🚀 Broker',      minGas:2000000,    tapBonus:8,  maxEnergy:4000 },
  { level:7, name:'👑 Mogul',       minGas:10000000,   tapBonus:12, maxEnergy:5000 },
  { level:8, name:'🌌 Legend',      minGas:50000000,   tapBonus:20, maxEnergy:7500 },
];

const UPGRADES = [
  {
    id:        'multi',
    icon:      '👆',
    name:      'Multi Tap',
    desc:      'Each tap earns more GAS',
    baseCost:  1000,
    costMult:  2.5,
    maxLevel:  20,
    effect:    'tapPower',
    perLevel:  1,
  },
  {
    id:        'energy',
    icon:      '⚡',
    name:      'Energy Limit',
    desc:      'Expand your max energy',
    baseCost:  2000,
    costMult:  2.2,
    maxLevel:  20,
    effect:    'maxEnergy',
    perLevel:  500,
  },
  {
    id:        'regen',
    icon:      '♻️',
    name:      'Energy Regen',
    desc:      'Energy recovers faster',
    baseCost:  3000,
    costMult:  2.0,
    maxLevel:  20,
    effect:    'energyRegen',
    perLevel:  1,
  },
  {
    id:        'passive',
    icon:      '🏦',
    name:      'Domain Farm',
    desc:      'Earn GAS while offline',
    baseCost:  5000,
    costMult:  2.8,
    maxLevel:  20,
    effect:    'passiveRate',
    perLevel:  200,
  },
  {
    id:        'refbonus',
    icon:      '👥',
    name:      'Ref Booster',
    desc:      'Earn more from referrals',
    baseCost:  8000,
    costMult:  3.0,
    maxLevel:  10,
    effect:    'refBonus',
    perLevel:  0.5,
  },
  {
    id:        'cipher',
    icon:      '🔐',
    name:      'Cipher Master',
    desc:      'Bigger daily cipher reward',
    baseCost:  10000,
    costMult:  3.5,
    maxLevel:  10,
    effect:    'cipherBonus',
    perLevel:  0.5,
  },
];

const DAILY_CIPHERS = [
  { day:0, hint:'.OG → .ETH → .NFT',   answer:['OG','ETH','NFT']   },
  { day:1, hint:'.SOL → .BNB → .ETH',  answer:['SOL','BNB','ETH']  },
  { day:2, hint:'.WEB3 → .OG → .SOL',  answer:['WEB3','OG','SOL']  },
  { day:3, hint:'.NFT → .WEB3 → .BNB', answer:['NFT','WEB3','BNB'] },
  { day:4, hint:'.ETH → .SOL → .OG',   answer:['ETH','SOL','OG']   },
  { day:5, hint:'.BNB → .NFT → .WEB3', answer:['BNB','NFT','WEB3'] },
  { day:6, hint:'.OG → .SOL → .ETH',   answer:['OG','SOL','ETH']   },
];

const TASKS = [
  { id:'daily_login',  name:'Daily Login',         reward:1000,   daily:true  },
  { id:'tap_100',      name:'Tap 100 Times',        reward:5000,   cond:'taps>=100'     },
  { id:'tap_1000',     name:'Tap 1,000 Times',      reward:25000,  cond:'taps>=1000'    },
  { id:'tap_10000',    name:'Tap 10,000 Times',     reward:100000, cond:'taps>=10000'   },
  { id:'invite_1',     name:'Invite 1 Friend',      reward:10000,  cond:'refs>=1'       },
  { id:'invite_5',     name:'Invite 5 Friends',     reward:60000,  cond:'refs>=5'       },
  { id:'invite_10',    name:'Invite 10 Friends',    reward:150000, cond:'refs>=10'      },
  { id:'reach_l2',     name:'Reach Level 2',        reward:20000,  cond:'level>=2'      },
  { id:'reach_l3',     name:'Reach Level 3',        reward:50000,  cond:'level>=3'      },
  { id:'buy_upgrade',  name:'Buy First Upgrade',    reward:15000,  cond:'anyUpgrade'    },
  { id:'endless_site', name:'Visit Endless Domains', reward:8000,  cond:'manual'        },
];

const REFERRAL_BONUS = {
  referrer:  10000,  // GAS the person who invited gets
  referred:  5000,   // GAS the new user gets for using a referral link
};

const BOOST = {
  dailyCount:  3,      // boosts reset to this every day
  multiplier:  5,      // 5x tap power during boost
  duration:    20,     // seconds
};

const CIPHER_BASE_REWARD = 50000;

// Pure helpers
function getUpgradeCost(upgradeId, currentLevel) {
  const upg = UPGRADES.find(u => u.id === upgradeId);
  if (!upg) return Infinity;
  return Math.floor(upg.baseCost * Math.pow(upg.costMult, currentLevel));
}

function getLevelForGas(totalGas) {
  let result = LEVELS[0];
  for (const lv of LEVELS) {
    if (totalGas >= lv.minGas) result = lv;
    else break;
  }
  return result;
}

function getTodayCipher() {
  const day = new Date().getDay();
  return DAILY_CIPHERS[day];
}

function computeTapPower(baseUser, upgradeLevels) {
  const multiLv = (upgradeLevels['multi'] || 0);
  const upg     = UPGRADES.find(u => u.id === 'multi');
  return 1 + multiLv * upg.perLevel;
}

function computePassiveRate(upgradeLevels) {
  const lv  = upgradeLevels['passive'] || 0;
  const upg = UPGRADES.find(u => u.id === 'passive');
  return lv * upg.perLevel; // gas per hour
}

function computeMaxEnergy(upgradeLevels) {
  const lv  = upgradeLevels['energy'] || 0;
  const upg = UPGRADES.find(u => u.id === 'energy');
  return 1000 + lv * upg.perLevel;
}

function computeEnergyRegen(upgradeLevels) {
  const lv  = upgradeLevels['regen'] || 0;
  const upg = UPGRADES.find(u => u.id === 'regen');
  return 1 + lv * upg.perLevel;
}

module.exports = {
  LEVELS, UPGRADES, DAILY_CIPHERS, TASKS, REFERRAL_BONUS,
  BOOST, CIPHER_BASE_REWARD,
  getUpgradeCost, getLevelForGas, getTodayCipher,
  computeTapPower, computePassiveRate, computeMaxEnergy, computeEnergyRegen,
};

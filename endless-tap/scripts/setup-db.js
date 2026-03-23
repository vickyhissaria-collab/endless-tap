// scripts/setup-db.js
// Run once: node scripts/setup-db.js
// Creates the SQLite database and all tables

require('dotenv').config();
const Database = require('better-sqlite3');
const path = require('path');
const fs   = require('fs');

const dbPath = process.env.DB_PATH || './data/endless.db';
const dir    = path.dirname(dbPath);
if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

const db = new Database(dbPath);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

console.log('🔧 Setting up database at', dbPath);

db.exec(`
  -- ─────────────────────────────────────────
  --  USERS
  -- ─────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS users (
    telegram_id     INTEGER PRIMARY KEY,
    username        TEXT,
    first_name      TEXT,
    last_name       TEXT,

    -- Core economy
    gas             INTEGER NOT NULL DEFAULT 0,
    total_taps      INTEGER NOT NULL DEFAULT 0,
    level           INTEGER NOT NULL DEFAULT 1,

    -- Tap stats
    tap_power       INTEGER NOT NULL DEFAULT 1,
    energy          INTEGER NOT NULL DEFAULT 1000,
    max_energy      INTEGER NOT NULL DEFAULT 1000,
    energy_regen    INTEGER NOT NULL DEFAULT 1,

    -- Passive income (gas per hour from upgrades)
    passive_rate    INTEGER NOT NULL DEFAULT 0,

    -- Boosts (reset daily)
    boosts_left     INTEGER NOT NULL DEFAULT 3,
    boosts_reset_at TEXT    NOT NULL DEFAULT (date('now')),

    -- Referral
    referred_by     INTEGER REFERENCES users(telegram_id),
    referral_count  INTEGER NOT NULL DEFAULT 0,
    referral_bonus_earned INTEGER NOT NULL DEFAULT 0,

    -- Daily cipher
    cipher_solved_at TEXT,
    cipher_date      TEXT,

    -- Timestamps
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    last_seen_at    TEXT NOT NULL DEFAULT (datetime('now')),
    last_tap_at     TEXT,

    -- Game meta
    total_gas_earned INTEGER NOT NULL DEFAULT 0
  );

  -- ─────────────────────────────────────────
  --  UPGRADES (per user, per upgrade type)
  -- ─────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS upgrades (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id     INTEGER NOT NULL REFERENCES users(telegram_id),
    upgrade_id      TEXT    NOT NULL,  -- 'multi', 'energy', 'regen', 'passive' etc
    level           INTEGER NOT NULL DEFAULT 0,
    bought_at       TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(telegram_id, upgrade_id)
  );

  -- ─────────────────────────────────────────
  --  TASKS (one-time and daily completions)
  -- ─────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS tasks (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id     INTEGER NOT NULL REFERENCES users(telegram_id),
    task_id         TEXT    NOT NULL,
    completed_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    gas_earned      INTEGER NOT NULL DEFAULT 0,
    UNIQUE(telegram_id, task_id)
  );

  -- ─────────────────────────────────────────
  --  REFERRALS (track each invite)
  -- ─────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS referrals (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    referrer_id     INTEGER NOT NULL REFERENCES users(telegram_id),
    referred_id     INTEGER NOT NULL REFERENCES users(telegram_id),
    bonus_paid      INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(referred_id)
  );

  -- ─────────────────────────────────────────
  --  TAP SESSIONS (rate limiting)
  -- ─────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS tap_sessions (
    telegram_id     INTEGER PRIMARY KEY REFERENCES users(telegram_id),
    taps_this_minute INTEGER NOT NULL DEFAULT 0,
    minute_start    TEXT    NOT NULL DEFAULT (datetime('now'))
  );

  -- ─────────────────────────────────────────
  --  INDEXES for speed
  -- ─────────────────────────────────────────
  CREATE INDEX IF NOT EXISTS idx_users_gas          ON users(gas DESC);
  CREATE INDEX IF NOT EXISTS idx_users_created      ON users(created_at);
  CREATE INDEX IF NOT EXISTS idx_referrals_referrer ON referrals(referrer_id);
  CREATE INDEX IF NOT EXISTS idx_upgrades_user      ON upgrades(telegram_id);
  CREATE INDEX IF NOT EXISTS idx_tasks_user         ON tasks(telegram_id);
`);

console.log('✅ Database tables created successfully');
console.log('🚀 Ready to run: npm start');
db.close();

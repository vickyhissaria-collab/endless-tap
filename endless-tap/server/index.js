// server/index.js
// Main server — starts Express, mounts routes, serves frontend

require('dotenv').config();
const express = require('express');
const cors    = require('cors');
const path    = require('path');

const routes  = require('./routes');

const app  = express();
const PORT = process.env.PORT || 3000;

// ── MIDDLEWARE ──
app.use(cors({
  origin: process.env.APP_URL || '*',
  methods: ['GET','POST'],
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ── STATIC FRONTEND ──
app.use(express.static(path.join(__dirname, '../frontend')));

// ── API ROUTES ──
app.use('/api', routes);

// ── HEALTH CHECK ──
app.get('/health', (req, res) => {
  res.json({ ok: true, ts: Date.now(), env: process.env.NODE_ENV || 'development' });
});

// ── CATCH-ALL → index.html (SPA) ──
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '../frontend/index.html'));
});

// ── GLOBAL ERROR HANDLER ──
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// ── START ──
app.listen(PORT, () => {
  console.log(`
╔═══════════════════════════════════════╗
║   ⬡  ENDLESS TAP SERVER STARTED       ║
║   Port:  ${PORT}                          ║
║   Env:   ${(process.env.NODE_ENV || 'development').padEnd(12)}           ║
╚═══════════════════════════════════════╝
  `);
});

module.exports = app;

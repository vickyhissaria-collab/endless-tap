# ⬡ ENDLESS TAP — Endless Domains
## Telegram Mini App — Tap-to-Earn Game

A real production backend + frontend for a viral Telegram tap-to-earn game.
Built on Node.js + SQLite. Deployable in under 30 minutes.

---

## WHAT THIS INCLUDES

- **Real user database** — every player stored with their gas, taps, upgrades
- **Real leaderboard** — actual other players, not fake numbers
- **Real referral system** — tracks who invited who, pays both parties
- **Telegram auth** — cryptographically verifies every request came from Telegram
- **Anti-cheat** — server-side tap rate limiting (max 600/minute)
- **Daily cipher** — new code every day, server-enforced one completion per day
- **Passive income** — offline earnings calculated on login
- **Boost system** — 3 boosts per day, resets at midnight
- **Task/mission system** — one-time and daily rewards
- **Level progression** — 8 ranks from Rookie to Legend

---

## STACK

| Layer     | Tech          |
|-----------|---------------|
| Runtime   | Node.js 18+   |
| Framework | Express       |
| Database  | SQLite (better-sqlite3) |
| Auth      | Telegram WebApp HMAC |
| Frontend  | Vanilla JS    |
| Hosting   | Any VPS / Railway / Render |

---

## STEP 1 — CREATE YOUR TELEGRAM BOT

1. Open Telegram and message `@BotFather`
2. Send `/newbot`
3. Choose a name: `Endless Domains`
4. Choose a username: `EndlessDomainBot` (or whatever is available)
5. **Copy the bot token** — looks like `7234567890:ABCdef...`

---

## STEP 2 — SET UP THE MINI APP

1. Message `@BotFather` again
2. Send `/newapp`
3. Select your bot
4. Set title: `ENDLESS TAP`
5. Set URL: `https://tap.endlessdomains.io` (your server URL — set this up in Step 4 first)
6. BotFather gives you a Mini App link like `t.me/EndlessDomainBot/tap`

---

## STEP 3 — INSTALL AND CONFIGURE

```bash
# Clone or upload these files to your server

cd endless-tap

# Install dependencies
npm install

# Copy env template
cp .env.example .env

# Edit .env with your values
nano .env
```

Your `.env` should look like:
```
BOT_TOKEN=7234567890:ABCdefGHIjklMNOpqrSTUvwxYZ
PORT=3000
APP_URL=https://tap.endlessdomains.io
DB_PATH=./data/endless.db
```

```bash
# Create the database
npm run setup-db

# Start the server
npm start
```

You should see:
```
╔═══════════════════════════════════════╗
║   ⬡  ENDLESS TAP SERVER STARTED       ║
║   Port:  3000                          ║
╚═══════════════════════════════════════╝
```

---

## STEP 4 — DEPLOY TO THE INTERNET

Your server needs to be accessible via HTTPS. Telegram requires HTTPS for Mini Apps.

### Option A — Railway (easiest, free tier available)
1. Go to railway.app
2. New Project → Deploy from GitHub (push your code to GitHub first)
3. Add environment variables in Railway dashboard
4. Railway gives you an HTTPS URL automatically

### Option B — VPS with nginx
```bash
# On your VPS (Ubuntu)
sudo apt install nginx certbot python3-certbot-nginx nodejs npm

# Install PM2 to keep server running
npm install -g pm2
pm2 start server/index.js --name endless-tap
pm2 save
pm2 startup

# Nginx config — create /etc/nginx/sites-available/endless-tap
server {
    server_name tap.endlessdomains.io;
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
    }
}

# Enable it
sudo ln -s /etc/nginx/sites-available/endless-tap /etc/nginx/sites-enabled/
sudo certbot --nginx -d tap.endlessdomains.io
sudo nginx -s reload
```

### Option C — Render.com (free)
1. render.com → New Web Service
2. Connect your GitHub repo
3. Build Command: `npm install && npm run setup-db`
4. Start Command: `npm start`
5. Add environment variables

---

## STEP 5 — UPDATE BOTFATHER WITH YOUR URL

1. Message `@BotFather`
2. Send `/myapps`
3. Select your app
4. Edit URL → paste your HTTPS server URL
5. Done — your Mini App is live!

---

## API ENDPOINTS

All endpoints require `X-Telegram-Init-Data` header (Telegram auth).

| Method | Path            | Description                        |
|--------|-----------------|------------------------------------|
| POST   | /api/login      | Login / register, get full state   |
| POST   | /api/tap        | Record taps (body: {taps: number}) |
| POST   | /api/upgrade    | Buy upgrade (body: {upgrade_id})   |
| POST   | /api/boost      | Activate daily boost               |
| POST   | /api/cipher     | Submit cipher answer               |
| POST   | /api/task       | Claim task reward                  |
| GET    | /api/leaderboard| Top 100 players                    |
| GET    | /api/referrals  | Your referral list                 |
| GET    | /api/stats      | Global player stats                |
| GET    | /health         | Health check                       |

---

## DEV MODE (local testing without Telegram)

The server accepts a mock user header when no BOT_TOKEN is set:

```bash
# In .env leave BOT_TOKEN as the placeholder value
# Then test with curl:
curl -X POST http://localhost:3000/api/login \
  -H "X-Mock-User-Id: 12345678" \
  -H "Content-Type: application/json"
```

Or open `http://localhost:3000` in browser directly — the frontend will use the mock user automatically.

---

## VIRAL GROWTH TIPS

1. **Share link format**: `t.me/EndlessDomainBot/tap?startapp=ref_USERID`
   — the `ref_` param auto-credits the referrer on signup

2. **Announce in your Telegram channel** with this message:
   > ⬡ ENDLESS TAP is live! Mine GAS tokens, climb the leaderboard, win real domain credits.
   > First 1,000 players get a legendary bonus. Join now: t.me/EndlessDomainBot/tap

3. **Daily cipher** is your retention hook — announce the hint in your channel each day, make players come back to solve it

4. **Leaderboard prizes** — announce top 3 win actual domain credits. Real stakes = real competition.

5. **Add a Telegram Bot command** for /stats so players can check rank in chat

---

## FILE STRUCTURE

```
endless-tap/
├── server/
│   ├── index.js        ← Express server entry
│   ├── routes.js       ← All API endpoints
│   ├── db.js           ← Database operations
│   ├── auth.js         ← Telegram auth validation
│   └── game-config.js  ← All game balance (levels, costs, etc)
├── frontend/
│   └── index.html      ← Complete game UI (single file)
├── scripts/
│   └── setup-db.js     ← Run once to create database
├── data/               ← Created automatically, holds endless.db
├── package.json
├── .env.example
└── README.md
```

---

## CUSTOMISING THE GAME

**Change tap rewards** → `server/game-config.js` → `UPGRADES` array → `perLevel` values

**Change level thresholds** → `server/game-config.js` → `LEVELS` array → `minGas`

**Change daily cipher** → `server/game-config.js` → `DAILY_CIPHERS` array

**Change referral bonus** → `server/game-config.js` → `REFERRAL_BONUS`

**Change base cipher reward** → `server/game-config.js` → `CIPHER_BASE_REWARD`

All game balance is in one file. No need to touch routes or DB.

---

## SUPPORT

Built for Endless Domains — endlessdomains.io

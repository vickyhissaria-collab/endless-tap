const crypto = require('crypto');

function validateTelegramData(initData, botToken) {
    try {
          const params = new URLSearchParams(initData);
          const hash = params.get('hash');
          if (!hash) return null;
          params.delete('hash');
          const arr = [];
          params.forEach((v, k) => arr.push(`${k}=${v}`));
          arr.sort();
          const secret = crypto.createHmac('sha256', 'WebAppData').update(botToken).digest();
          const computed = crypto.createHmac('sha256', secret).update(arr.join('\n')).digest('hex');
          if (computed !== hash) return null;
          const u = params.get('user');
          return u ? JSON.parse(u) : null;
    } catch(e) { return null; }
}

function requireAuth(req, res, next) {
    const botToken = process.env.BOT_TOKEN;

  // Try Telegram initData first
  const initData = req.headers['x-telegram-init-data'] || req.body?.initData;
    if (initData && initData.length > 10 && botToken && botToken !== 'your_bot_token_here') {
          const user = validateTelegramData(initData, botToken);
          if (user) { req.tgUser = user; return next(); }
    }

  // Accept mock user header (for dev and fallback)
  const mockId = req.headers['x-mock-user-id'];
    if (mockId) {
          req.tgUser = { id: parseInt(mockId), first_name: 'Player', username: 'player' };
          return next();
    }

  // Create anonymous session so game loads even without auth
  req.tgUser = { id: Math.floor(Math.random() * 9000000 + 1000000), first_name: 'Player', username: null };
    next();
}

module.exports = { requireAuth, validateTelegramData };

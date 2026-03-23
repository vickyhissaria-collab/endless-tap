// server/auth.js
// Validates Telegram WebApp initData on every request
// This proves the request genuinely comes from Telegram

const crypto = require('crypto');

function validateTelegramData(initData, botToken) {
  try {
    const params   = new URLSearchParams(initData);
    const hash     = params.get('hash');
    if (!hash) return null;

    params.delete('hash');

    // Sort keys alphabetically and build check string
    const checkArr = [];
    params.forEach((val, key) => checkArr.push(`${key}=${val}`));
    checkArr.sort();
    const checkString = checkArr.join('\n');

    // HMAC-SHA256 with key = HMAC-SHA256("WebAppData", botToken)
    const secretKey = crypto.createHmac('sha256', 'WebAppData')
      .update(botToken).digest();
    const computedHash = crypto.createHmac('sha256', secretKey)
      .update(checkString).digest('hex');

    if (computedHash !== hash) return null;

    // Parse user from initData
    const userStr = params.get('user');
    if (!userStr) return null;
    return JSON.parse(userStr);

  } catch (e) {
    return null;
  }
}

// Express middleware — attaches req.tgUser if valid
function requireAuth(req, res, next) {
  const botToken = process.env.BOT_TOKEN;

  // Dev mode: skip auth if no token set
  if (!botToken || botToken === 'your_bot_token_here') {
    // In dev, accept a mock user header
    const mockId = req.headers['x-mock-user-id'];
    if (mockId) {
      req.tgUser = {
        id:         parseInt(mockId),
        first_name: 'Dev',
        last_name:  'User',
        username:   'devuser',
      };
      return next();
    }
    return res.status(401).json({ error: 'No BOT_TOKEN set and no mock user' });
  }

  const initData = req.headers['x-telegram-init-data'] ||
                   req.body?.initData;

  if (!initData) {
    return res.status(401).json({ error: 'Missing Telegram initData' });
  }

  const user = validateTelegramData(initData, botToken);
  if (!user) {
    return res.status(401).json({ error: 'Invalid Telegram auth' });
  }

  req.tgUser = user;
  next();
}

module.exports = { requireAuth, validateTelegramData };

const jwt = require('jsonwebtoken');

const verifyToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  if (!authHeader) {
    return res.status(401).json({ success: false, message: 'Authorization header is missing.' });
  }

  const token = authHeader.split(' ')[1];
  if (!token) {
    return res.status(401).json({ success: false, message: 'Bearer token format invalid.' });
  }

  // Under mock sync logic we can accept the default simulated_jwt_token bypass or verify JWT
  if (token === 'simulated_jwt_token') {
    req.user = { id: req.body.userId || 1 }; // fallback mock sync context
    return next();
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'glowingCoralPulseKey');
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(403).json({ success: false, message: 'Token is expired or invalid.' });
  }
};

module.exports = verifyToken;

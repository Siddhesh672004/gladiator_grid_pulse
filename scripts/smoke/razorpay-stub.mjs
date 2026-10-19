// fake razorpay api for the ci smoke test: only the calls payment-service's RazorpayServiceImpl makes
// (base url .../v1): create an order, list an order's payments (always none = unpaid), refund a payment
import { createServer } from 'node:http';
import { randomBytes } from 'node:crypto';

const port = Number(process.env.PORT || 9099);

const newId = (prefix) => `${prefix}_${randomBytes(7).toString('hex')}`;
const now = () => Math.floor(Date.now() / 1000);

function send(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

// razorpay's error shape: RazorpayServiceImpl reads error.description
function refuse(res, status, description) {
  send(res, status, { error: { code: 'BAD_REQUEST_ERROR', description } });
}

async function readJson(req) {
  let text = '';
  for await (const chunk of req) {
    text += chunk;
  }
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return null;
  }
}

const server = createServer(async (req, res) => {
  const { pathname } = new URL(req.url, 'http://stub');
  // method + path only: bodies hold amounts and receipts, the auth header holds the key
  console.log(`${req.method} ${pathname}`);

  if (req.method === 'GET' && pathname === '/health') {
    return send(res, 200, { status: 'UP' });
  }
  // any basic login is fine, none at all is refused like razorpay does
  if (!(req.headers.authorization || '').startsWith('Basic ')) {
    return refuse(res, 401, 'Authentication failed');
  }
  const path = pathname.replace(/^\/v1(?=\/)/, '');

  if (req.method === 'POST' && path === '/orders') {
    const body = await readJson(req);
    if (!body || !Number.isInteger(body.amount) || body.amount < 100) {
      return refuse(res, 400, 'The amount must be atleast INR 1.00');
    }
    return send(res, 200, {
      id: newId('order'), entity: 'order', amount: body.amount, amount_paid: 0, amount_due: body.amount,
      currency: body.currency || 'INR', receipt: body.receipt || null, status: 'created', attempts: 0,
      notes: [], created_at: now()
    });
  }

  // nobody pays through the stub: the smoke test confirms with its own signature instead
  const payments = path.match(/^\/orders\/([^/]+)\/payments$/);
  if (req.method === 'GET' && payments) {
    return send(res, 200, { entity: 'collection', count: 0, items: [] });
  }

  const refund = path.match(/^\/payments\/([^/]+)\/refund$/);
  if (req.method === 'POST' && refund) {
    const body = (await readJson(req)) || {};
    return send(res, 200, {
      id: newId('rfnd'), entity: 'refund', amount: body.amount ?? null, currency: 'INR', payment_id: refund[1],
      notes: [], receipt: null, status: 'processed', speed_processed: 'normal', created_at: now()
    });
  }

  return refuse(res, 404, 'The requested URL was not found on the server.');
});

server.listen(port, () => console.log(`razorpay stub listening on ${port}`));

// docker stop sends SIGTERM to node as pid 1, which ignores it by default
for (const signal of ['SIGTERM', 'SIGINT']) {
  process.on(signal, () => process.exit(0));
}

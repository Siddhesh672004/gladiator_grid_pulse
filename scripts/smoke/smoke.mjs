// end-to-end smoke test of the running compose stack (ci): accounts, pay + book, approve / reject + refund, live board,
// feedback, faq chat, error shapes and the core's api list. one line per step, exit 1 at the first failure
// never prints tokens, codes or passwords; out/session.json (tokens + ids) is for scripts/ci/screenshots.mjs
import { createHmac, randomBytes, randomInt } from 'node:crypto';
import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = join(HERE, 'out');
const BASELINE = join(HERE, 'baseline', 'core-openapi.json');
const GATEWAY = (process.env.GATEWAY || 'http://localhost:8080').replace(/\/+$/, '');
const CORE = (process.env.CORE || 'http://localhost:8082').replace(/\/+$/, '');
const MAIL_DIR = resolve(process.env.MAIL_DIR || join(OUT_DIR, 'mails'));
const { RAZORPAY_KEY_SECRET, SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD } = process.env;

// own names, emails and numbers per run: a second run against the same database does not clash
const RUN = Date.now().toString(36);
const PRICE = 500;
const ERROR_KEYS = ['timestamp', 'status', 'error', 'message', 'path'];
const HTTP_METHODS = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace'];
// 1x1 png: the upload only checks type and size before it finds out cloudinary has no keys
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
  'base64');

const session = { gateway: GATEWAY, run: RUN, drivers: [] };
let phase = 'start';

const sleep = (ms) => new Promise((done) => setTimeout(done, ms));

function pass(step, detail = '') {
  console.log(`PASS  ${step}${detail ? `  (${detail})` : ''}`);
}

function fail(step, detail) {
  console.log(`FAIL  ${step}  ${detail}`);
  process.exit(1);
}

function check(step, ok, detail) {
  if (!ok) {
    fail(step, detail);
  }
}

// the status + the server's own message, never what was sent
function describe(res) {
  const message = typeof res.json?.message === 'string' ? `: ${res.json.message.slice(0, 160)}` : '';
  return `HTTP ${res.status}${message}`;
}

async function call(method, url, { token, body, form } = {}) {
  const headers = { Accept: 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  let payload;
  if (form) {
    payload = form;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(url, { method, headers, body: payload, signal: AbortSignal.timeout(30_000) });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }
  return { status: res.status, json };
}

// everything the browser does goes through the gateway
const api = (method, path, options) => call(method, GATEWAY + path, options);

function expectStatus(step, res, ...wanted) {
  check(step, wanted.includes(res.status), `${describe(res)}, expected ${wanted.join(' or ')}`);
  return res.json;
}

function saveSession() {
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(join(OUT_DIR, 'session.json'), JSON.stringify(session, null, 2));
}

// session dates are india time (RaceSession.today()), the runner clock is utc
function indiaDate(daysAhead) {
  return new Date(Date.now() + 330 * 60_000 + daysAhead * 86_400_000).toISOString().slice(0, 10);
}

async function waitForHealth() {
  phase = 'health';
  const deadline = Date.now() + 5 * 60_000;
  for (const [name, base] of [['gateway', GATEWAY], ['core', CORE]]) {
    let last = 'no answer';
    for (;;) {
      try {
        const res = await call('GET', `${base}/actuator/health`);
        if (res.status === 200 && res.json?.status === 'UP') {
          break;
        }
        last = `HTTP ${res.status}`;
      } catch (error) {
        last = error.cause?.code || error.name;
      }
      check(`health ${name}`, Date.now() < deadline, `not UP after 5 minutes (${last})`);
      await sleep(5000);
    }
    pass(`health ${name}`, 'UP');
  }
}

// the gateway allows 5 logins a minute per address, this run makes 3
async function login(step, path, email, password) {
  const json = expectStatus(step, await api('POST', path, { body: { email, password } }), 200);
  check(step, typeof json?.token === 'string' && json.token.length > 20, 'no token in the answer');
  pass(step, `${json.userRole} #${json.userId}`);
  return { token: json.token, userId: json.userId, username: json.username, userRole: json.userRole };
}

function trackRequest() {
  return {
    trackName: `Smoke Ring ${RUN}`,
    location: 'Test Valley, Pune',
    description: 'A short test circuit made by the CI smoke test.',
    trackCategory: 'Circuit',
    isIndoor: false,
    openTime: '06:00',
    closeTime: '22:00',
    facilities: 'Pit lane, paddock, cafe',
    // without cloudinary keys any cloudinary link counts as one of ours (CloudinaryServiceImpl.isOurImage)
    imageUrls: ['https://res.cloudinary.com/demo/image/upload/sample.jpg']
  };
}

async function createTrackAndSessions(admin) {
  phase = 'track + sessions';
  let step = 'admin creates a track';
  const track = expectStatus(step, await api('POST', '/api/race-track', { token: admin.token, body: trackRequest() }), 201);
  check(step, Number.isInteger(track?.trackId), 'no trackId');
  session.trackId = track.trackId;
  pass(step, `track #${track.trackId}`);

  // tomorrow: bookable now (booking closes at the start time), race control may start it any time
  step = 'admin creates a practice session';
  const flow = { trackId: track.trackId, sessionDate: indiaDate(1), startTime: '10:00', endTime: '11:00', price: PRICE,
    maxCars: 10 };
  const created = expectStatus(step, await api('POST', '/api/sessions', { token: admin.token, body: flow }), 201);
  check(step, created?.status === 'SCHEDULED', `session is ${created?.status}`);
  session.sessionId = created.sessionId;
  pass(step, `session #${created.sessionId} on ${flow.sessionDate} 10:00-11:00 IST`);

  // stays open, so the track still shows up for drivers on the screenshots
  step = 'admin creates a second session';
  const open = { ...flow, startTime: '14:00', endTime: '15:00', price: 750, maxCars: 12 };
  const second = expectStatus(step, await api('POST', '/api/sessions', { token: admin.token, body: open }), 201);
  session.openSessionId = second.sessionId;
  pass(step, `session #${second.sessionId} 14:00-15:00 IST`);
  saveSession();
}

function newDriver(n) {
  return {
    username: `Smoke Driver ${n === 1 ? 'One' : 'Two'}`,
    email: `smoke.d${n}.${RUN}@gridpulse.test`,
    // 10 digits starting with 9, unique per run
    mobileNumber: `9${randomInt(100_000_000, 1_000_000_000)}`,
    dateOfBirth: n === 1 ? '1994-04-14' : '1997-07-17',
    // DL + 4 digits, the first digit keeps the two drivers apart
    driverLicenseNumber: `DL${n}${randomInt(100, 1000)}`,
    // upper, lower, digit, special, no spaces (ValidationRules)
    password: `Gp!${randomBytes(6).toString('hex')}Zz9`
  };
}

// MAIL_TO_FILE: notification-service writes <yyyyMMdd-HHmmss-SSS>_<to>.txt, the newest one for the address counts
async function waitForCode(step, email) {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    const files = existsSync(MAIL_DIR)
      ? readdirSync(MAIL_DIR).filter((name) => name.endsWith(`_${email}.txt`)).sort()
      : [];
    const code = files.length
      ? readFileSync(join(MAIL_DIR, files.at(-1)), 'utf8').match(/verification code: (\d{6})/)?.[1]
      : undefined;
    if (code) {
      return code;
    }
    await sleep(1000);
  }
  return fail(step, `no code email in ${MAIL_DIR} after 90 s`);
}

async function signUp(n, driver) {
  phase = `driver ${n} sign up`;
  const step = `driver ${n} signs up`;
  const sent = expectStatus(`${step}: send code`, await api('POST', '/api/register/send-code', { body: driver }), 200);
  check(`${step}: send code`, sent?.expiresInSeconds > 0, 'no new code was sent');
  const otp = await waitForCode(`${step}: code email`, driver.email);
  const user = expectStatus(`${step}: register`, await api('POST', '/api/register', { body: { ...driver, otp } }), 201);
  pass(step, `code mailed, account #${user?.userId}`);
}

// sign up already saved date of birth + license, the profile page sends the same values again
async function completeProfile(n, driver, account) {
  phase = `driver ${n} profile`;
  const step = `driver ${n} profile`;
  const body = { dateOfBirth: driver.dateOfBirth, driverLicenseNumber: driver.driverLicenseNumber };
  const profile = expectStatus(step, await api('PUT', '/api/profile', { token: account.token, body }), 200);
  check(step, profile?.profileComplete === true, 'profile is not complete');
  pass(step, 'complete');
}

// 202 = the core was slow, payment-service's job finishes the booking: wait until it shows up
async function waitForBooking(step, account, carNumber) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    await sleep(3000);
    const mine = expectStatus(step, await api('GET', `/api/race-booking/user/${account.userId}`,
      { token: account.token }), 200);
    const found = (mine ?? []).find((booking) => booking.session?.sessionId === session.sessionId
      && booking.carNumber === carNumber);
    if (found) {
      return found;
    }
  }
  return fail(step, 'confirm answered 202 and no booking appeared within 120 s');
}

async function payAndRegister(n, account, carNumber, carCategory) {
  phase = `driver ${n} pay & register`;
  const step = `driver ${n} pays + registers`;
  const booking = { sessionId: session.sessionId, carNumber, carCategory, comments: 'Booked by the CI smoke test',
    termsAccepted: true };
  const order = expectStatus(`${step}: order`, await api('POST', '/api/payments/order',
    { token: account.token, body: booking }), 201);
  check(`${step}: order`, /^order_/.test(order?.orderId ?? ''), 'no razorpay order id');
  check(`${step}: order`, order.amount === PRICE * 100, `amount ${order.amount} paise, expected ${PRICE * 100}`);

  // what the razorpay window hands back: HMAC-SHA256(orderId|paymentId, key secret) as hex
  const paymentId = `pay_${randomBytes(7).toString('hex')}`;
  const signature = createHmac('sha256', RAZORPAY_KEY_SECRET).update(`${order.orderId}|${paymentId}`).digest('hex');
  const res = await api('POST', '/api/payments/confirm', { token: account.token, body: { booking,
    razorpayOrderId: order.orderId, razorpayPaymentId: paymentId, razorpaySignature: signature } });
  let made = expectStatus(`${step}: confirm`, res, 201, 202);
  if (res.status === 202) {
    made = await waitForBooking(`${step}: confirm`, account, carNumber);
  }
  check(step, made?.status === 'Pending' && made?.paymentStatus === 'PAID',
    `booking is ${made?.status} / ${made?.paymentStatus}, expected Pending / PAID`);
  check(step, made.carNumber === carNumber, `car #${made.carNumber}, expected #${carNumber}`);
  pass(step, `HTTP ${res.status}, booking #${made.raceBookingId} car #${carNumber} Pending + PAID`);
  return made.raceBookingId;
}

async function decideBookings(admin, rejectedId, approvedId) {
  phase = 'approve / reject';
  let step = 'admin approves driver 2';
  const approved = expectStatus(step, await api('PUT', `/api/race-booking/${approvedId}`,
    { token: admin.token, body: { status: 'Approved' } }), 200);
  check(step, approved?.status === 'Approved', `booking is ${approved?.status}`);
  pass(step, `booking #${approvedId} Approved`);

  step = 'admin rejects driver 1';
  const rejected = expectStatus(step, await api('PUT', `/api/race-booking/${rejectedId}`,
    { token: admin.token, body: { status: 'Rejected', adminNote: 'Smoke test: rejected on purpose' } }), 200);
  check(step, rejected?.status === 'Rejected' && rejected?.paymentStatus === 'REFUND_PENDING',
    `booking is ${rejected?.status} / ${rejected?.paymentStatus}, expected Rejected / REFUND_PENDING`);
  pass(step, `booking #${rejectedId} Rejected + REFUND_PENDING`);

  // core outbox -> kafka -> payment-service -> RefundJob (every 30 s) -> stub refund -> kafka -> core
  phase = 'refund';
  step = 'driver 1 refund';
  const started = Date.now();
  for (;;) {
    const status = expectStatus(step, await api('GET', `/api/race-booking/${rejectedId}`, { token: admin.token }),
      200)?.paymentStatus;
    if (status === 'REFUNDED') {
      pass(step, `REFUNDED after ${Math.round((Date.now() - started) / 1000)} s`);
      return;
    }
    check(step, status !== 'REFUND_FAILED', 'payment-service marked the refund REFUND_FAILED');
    check(step, Date.now() - started < 120_000, `still ${status} after 120 s`);
    await sleep(3000);
  }
}

// a small server-sent events reader on fetch: nextBoard() = the next "standings" data, null after the timeout
async function openLiveBoard(url) {
  const controller = new AbortController();
  const res = await fetch(url, { headers: { Accept: 'text/event-stream' }, signal: controller.signal });
  const boards = [];
  let wake = null;
  (async () => {
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
      const { done, value } = await reader.read();
      if (done) {
        return;
      }
      buffer += decoder.decode(value, { stream: true }).replace(/\r/g, '');
      let end;
      while ((end = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, end);
        buffer = buffer.slice(end + 2);
        // ":ping" comment lines carry no data
        const data = block.split('\n')
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5).replace(/^ /, ''))
          .join('\n');
        if (data) {
          boards.push(data);
          wake?.();
        }
      }
    }
  })().catch(() => {});

  async function nextBoard(timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (boards.length === 0) {
      const left = deadline - Date.now();
      if (left <= 0) {
        return null;
      }
      await new Promise((done) => {
        wake = done;
        setTimeout(done, left);
      });
      wake = null;
    }
    try {
      return JSON.parse(boards.shift());
    } catch {
      return [];
    }
  }

  return { status: res.status, nextBoard, close: () => controller.abort() };
}

async function raceControl(admin, carBookingId) {
  phase = 'live board';
  const carOf = (rows) => (Array.isArray(rows) ? rows.find((row) => row.raceBookingId === carBookingId) : undefined);
  let step = 'live board connects';
  const board = await openLiveBoard(`${GATEWAY}/api/live/session/${session.sessionId}`);
  if (board.status !== 200) {
    board.close();
    fail(step, `HTTP ${board.status}`);
  }
  const first = await board.nextBoard(20_000);
  check(step, Array.isArray(first), 'no board within 20 s of connecting');
  check(step, carOf(first)?.raceState === 'NOT_STARTED',
    `approved car is ${carOf(first)?.raceState ?? 'missing'} before the start`);
  pass(step, `${first.length} car(s) on the board`);

  step = 'race control start';
  const started = expectStatus(step, await api('POST', `/api/race-control/${carBookingId}/start`,
    { token: admin.token }), 200);
  check(step, started?.raceState === 'RACING', `car is ${started?.raceState}`);
  pass(step, 'car RACING, session LIVE');

  // the stream may sit on the other core instance: then the change arrives over kafka
  step = 'live board push';
  const deadline = Date.now() + 30_000;
  let racing = false;
  while (!racing) {
    const next = await board.nextBoard(deadline - Date.now());
    if (next === null) {
      break;
    }
    racing = carOf(next)?.raceState === 'RACING';
  }
  board.close();
  check(step, racing, 'no board with the car RACING within 30 s of the start');
  pass(step, 'RACING pushed within 30 s');

  step = 'race control finish';
  const finished = expectStatus(step, await api('POST', `/api/race-control/session/${session.sessionId}/finish`,
    { token: admin.token }), 200);
  check(step, carOf(finished)?.raceState === 'FINISHED' && carOf(finished)?.finalPosition === 1,
    `car is ${carOf(finished)?.raceState} P${carOf(finished)?.finalPosition}`);
  pass(step, 'car FINISHED P1, session COMPLETED');
}

async function giveFeedback(account) {
  phase = 'feedback';
  const step = 'driver 2 feedback';
  const body = { sessionId: session.sessionId, rating: 5,
    feedbackText: 'Clean track, friendly marshals and a smooth start.' };
  const res = await api('POST', '/api/feedback', { token: account.token, body });
  expectStatus(step, res, 201, 200);
  pass(step, `HTTP ${res.status}`);
}

// no ai keys in ci: the faq word search answers
async function faqChat() {
  phase = 'chat';
  const step = 'faq chat';
  const reply = expectStatus(step, await api('POST', '/api/chat', { body: { message: 'How do I book a race session?' } }),
    200);
  check(step, typeof reply?.reply === 'string' && reply.reply.trim().length > 0, 'empty reply');
  pass(step, `answer from ${reply.source}`);
}

function expectError(step, res, wanted) {
  check(step, res.status === wanted, `${describe(res)}, expected ${wanted}`);
  const missing = ERROR_KEYS.filter((key) => !(res.json && key in res.json));
  check(step, missing.length === 0, `error body has no ${missing.join(', ')}`);
  check(step, res.json.status === wanted, `body says status ${res.json.status}`);
  pass(step, `HTTP ${wanted}, ErrorResponse shape`);
}

async function errorShapes(admin, driver) {
  phase = 'error shapes';
  expectError('error 400 (invalid track)', await api('POST', '/api/race-track', { token: admin.token, body: {} }), 400);
  expectError('error 401 (no token)', await api('GET', '/api/profile'), 401);
  expectError('error 403 (driver on admin list)', await api('GET', '/api/race-booking', { token: driver.token }), 403);
  expectError('error 404 (unknown booking)', await api('GET', '/api/race-booking/987654321', { token: admin.token }),
    404);
  expectError('error 409 (same track name)', await api('POST', '/api/race-track',
    { token: admin.token, body: trackRequest() }), 409);

  // a real picture type, so the upload gets as far as "cloudinary has no keys"
  const form = new FormData();
  form.append('file', new Blob([PNG], { type: 'image/png' }), 'smoke.png');
  expectError('error 503 (picture upload without cloudinary)', await api('POST', '/api/images/track',
    { token: admin.token, form }), 503);

  // last: 3 send-code calls per 10 minutes per address, the two sign ups used two
  let res;
  for (let tries = 0; tries < 6; tries++) {
    res = await api('POST', '/api/register/send-code', { body: {} });
    if (res.status === 429) {
      break;
    }
  }
  expectError('error 429 (send-code limit)', res, 429);
}

// only what the api offers: sorted "METHOD path" pairs and schema names (no servers, ids or texts)
function normaliseApi(doc) {
  const operations = Object.entries(doc?.paths ?? {})
    .flatMap(([path, item]) => Object.keys(item)
      .filter((key) => HTTP_METHODS.includes(key))
      .map((method) => `${method.toUpperCase()} ${path}`))
    .sort();
  const schemas = Object.keys(doc?.components?.schemas ?? {}).sort();
  return { operations, schemas };
}

function apiDiff(baseline, current) {
  return ['operations', 'schemas'].flatMap((key) => {
    const before = new Set(baseline[key] ?? []);
    const now = new Set(current[key]);
    return [...[...before].filter((item) => !now.has(item)).map((item) => `- ${key}: ${item}`),
      ...[...now].filter((item) => !before.has(item)).map((item) => `+ ${key}: ${item}`)];
  });
}

// core-1 directly: springdoc is on outside prod, the gateway does not route /v3/**
async function checkApiDocs() {
  phase = 'openapi';
  const step = 'core openapi';
  const current = normaliseApi(expectStatus(step, await call('GET', `${CORE}/v3/api-docs`), 200));
  check(step, current.operations.length > 0, 'no operations in /v3/api-docs');
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(join(OUT_DIR, 'core-openapi.json'), `${JSON.stringify(current, null, 2)}\n`);
  if (!existsSync(BASELINE)) {
    pass(step, `recorded ${current.operations.length} operations + ${current.schemas.length} schemas `
      + 'to out/core-openapi.json (no baseline yet)');
    return;
  }
  const diff = apiDiff(JSON.parse(readFileSync(BASELINE, 'utf8')), current);
  diff.slice(0, 60).forEach((line) => console.log(`      ${line}`));
  check(step, diff.length === 0, `${diff.length} difference(s) from baseline/core-openapi.json`);
  pass(step, `same as the baseline (${current.operations.length} operations, ${current.schemas.length} schemas)`);
}

async function main() {
  const missing = ['RAZORPAY_KEY_SECRET', 'SUPER_ADMIN_EMAIL', 'SUPER_ADMIN_PASSWORD'].filter((name) => !process.env[name]);
  check('settings', missing.length === 0, `missing env ${missing.join(', ')}`);
  console.log(`smoke test: gateway ${GATEWAY}, core ${CORE}, mails ${MAIL_DIR}`);

  await waitForHealth();

  phase = 'admin login';
  const admin = await login('super admin logs in', '/api/admin/login', SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);
  session.admin = admin;
  saveSession();
  await createTrackAndSessions(admin);

  const drivers = [newDriver(1), newDriver(2)];
  for (const [i, driver] of drivers.entries()) {
    await signUp(i + 1, driver);
  }
  const accounts = [];
  for (const [i, driver] of drivers.entries()) {
    phase = `driver ${i + 1} login`;
    accounts.push(await login(`driver ${i + 1} logs in`, '/api/login', driver.email, driver.password));
  }
  session.drivers = accounts;
  saveSession();

  for (const [i, driver] of drivers.entries()) {
    await completeProfile(i + 1, driver, accounts[i]);
  }
  accounts[0].bookingId = await payAndRegister(1, accounts[0], 11, 'GT3');
  accounts[1].bookingId = await payAndRegister(2, accounts[1], 22, 'Formula');
  saveSession();

  await decideBookings(admin, accounts[0].bookingId, accounts[1].bookingId);
  await raceControl(admin, accounts[1].bookingId);
  await giveFeedback(accounts[1]);
  await faqChat();
  await errorShapes(admin, accounts[0]);
  await checkApiDocs();
}

main().then(() => {
  console.log('smoke test passed');
  process.exit(0);
}, (error) => fail(phase, `unexpected error: ${error?.message ?? error}${error?.cause?.code ? ` (${error.cause.code})` : ''}`));

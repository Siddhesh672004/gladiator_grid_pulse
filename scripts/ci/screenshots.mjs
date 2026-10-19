// ci only: serves the built angular site (WEB_DIST) on :4200 and screenshots the main pages with the smoke test's
// logins (scripts/smoke/out/session.json); a page that fails is logged and skipped, the script never fails the job
import { createServer } from 'node:http';
import { existsSync, mkdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, extname, join, normalize, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const WEB_DIST = resolve(process.env.WEB_DIST || 'web-dist');
const SESSION_FILE = join(HERE, '..', 'smoke', 'out', 'session.json');
const SHOTS = join(HERE, '..', 'smoke', 'out', 'screenshots');
const PORT = 4200;
const SITE = `http://localhost:${PORT}`;

const VIEWPORTS = [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 390, height: 844 }
];

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg', '.webp': 'image/webp', '.gif': 'image/gif', '.ico': 'image/x-icon', '.woff': 'font/woff',
  '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.txt': 'text/plain; charset=utf-8', '.map': 'application/json'
};

// a file of the build, or index.html for an angular route (spa fallback); nothing outside WEB_DIST
function fileFor(urlPath) {
  const wanted = normalize(join(WEB_DIST, decodeURIComponent(urlPath)));
  const inside = wanted === WEB_DIST || wanted.startsWith(WEB_DIST + sep);
  return inside && existsSync(wanted) && statSync(wanted).isFile() ? wanted : join(WEB_DIST, 'index.html');
}

function startSite() {
  const server = createServer((req, res) => {
    try {
      const file = fileFor(new URL(req.url, SITE).pathname);
      res.writeHead(200, { 'Content-Type': TYPES[extname(file).toLowerCase()] || 'application/octet-stream' });
      res.end(readFileSync(file));
    } catch {
      res.writeHead(500);
      res.end();
    }
  });
  return new Promise((done) => server.listen(PORT, () => done(server)));
}

function readSession() {
  try {
    return JSON.parse(readFileSync(SESSION_FILE, 'utf8'));
  } catch {
    console.log('no smoke session found: only the public pages');
    return {};
  }
}

// runs in the page before angular starts: the same localStorage keys UserStorageService.saveLogin writes
function writeLogin(saved) {
  localStorage.setItem('token', saved.token);
  localStorage.setItem('username', saved.username);
  localStorage.setItem('userRole', saved.userRole);
  localStorage.setItem('userId', String(saved.userId));
}

function pagesFor(session) {
  const driver = session.drivers?.[1] ?? session.drivers?.[0];
  const pages = [
    { name: 'landing', path: '/' },
    { name: 'how-it-works', path: '/how-it-works' },
    { name: 'login', path: '/login' },
    { name: 'signup', path: '/signup' }
  ];
  if (driver?.token) {
    pages.push(
      { name: 'driver-home', path: '/home', account: driver },
      { name: 'driver-tracks', path: '/user/view/tracks', account: driver },
      { name: 'driver-register-race', path: `/user/register/${session.trackId}`, account: driver },
      { name: 'driver-registrations', path: '/user/view/registrations', account: driver },
      { name: 'driver-live-timing', path: '/user/live-timing', account: driver },
      { name: 'driver-stats', path: '/user/stats', account: driver });
  }
  if (session.admin?.token) {
    pages.push(
      { name: 'admin-dashboard', path: '/admin/dashboard', account: session.admin },
      { name: 'admin-registrations', path: '/admin/view/registrations', account: session.admin },
      { name: 'admin-race-control', path: '/admin/race-control', account: session.admin });
  }
  return pages;
}

async function shoot(browser, viewport, page) {
  const file = join(SHOTS, `${viewport.name}-${page.name}.png`);
  const context = await browser.newContext({ viewport: { width: viewport.width, height: viewport.height } });
  try {
    if (page.account) {
      const { token, username, userRole, userId } = page.account;
      await context.addInitScript(writeLogin, { token, username, userRole, userId });
    }
    const tab = await context.newPage();
    await tab.goto(SITE + page.path, { waitUntil: 'domcontentloaded', timeout: 30_000 });
    // live pages keep a stream open, so "network idle" may never come: give it a few seconds at most
    await tab.waitForLoadState('networkidle', { timeout: 8_000 }).catch(() => {});
    await tab.waitForTimeout(800);
    await tab.screenshot({ path: file, fullPage: true });
    console.log(`shot  ${viewport.name} ${page.name}`);
  } catch (error) {
    console.log(`skip  ${viewport.name} ${page.name}: ${error.message.split('\n')[0]}`);
  } finally {
    await context.close().catch(() => {});
  }
}

async function main() {
  if (!existsSync(join(WEB_DIST, 'index.html'))) {
    console.log(`no index.html in ${WEB_DIST}: nothing to screenshot`);
    return;
  }
  // installed in ci only (npm i --no-save playwright in scripts/ci), loaded here so a missing one is just a log line
  const { chromium } = await import('playwright');
  const pages = pagesFor(readSession());
  mkdirSync(SHOTS, { recursive: true });
  const server = await startSite();
  const browser = await chromium.launch();
  try {
    for (const viewport of VIEWPORTS) {
      for (const page of pages) {
        await shoot(browser, viewport, page);
      }
    }
  } finally {
    await browser.close().catch(() => {});
    server.close();
  }
}

main().catch((error) => console.log(`screenshots stopped: ${error.message.split('\n')[0]}`)).finally(() => process.exit(0));

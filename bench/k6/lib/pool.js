// Cross-VU shared state for the sender/receiver model.
//
// k6 does NOT share JS state between VUs -- each VU runs in its own isolated runtime, so
// a module-level array/object (like seed.js used to use for setup()/handleSummary())
// cannot coordinate "senders push a code, receivers pull a live code" across many VUs.
// The fix: a tiny local Redis on the load-gen EC2 instance itself, driven via k6's
// stock, bundled `k6/experimental/redis` client (no custom xk6 build needed). This Redis
// is purely load-gen coordination state -- it is NOT the app's Valkey and never touches
// the system under test. It's disposable: no persistence, wiped every run.
// NOTE on the k6/experimental/redis API surface: the typed convenience methods (sadd,
// srem, srandmember, expire) are stable across recent k6 releases per the official docs,
// but HSET/HINCRBY/HGET's exact typed signatures have shifted between k6 versions. To
// avoid depending on a signature that may not match the k6 binary actually installed on
// the load-gen instance, the hash operations below go through the universal
// `client.sendCommand(...)` escape hatch (always available, takes the raw Redis command
// name + args) instead of a typed method. Before relying on this in a real run, sanity
// check with `k6 version` on the instance and, if needed, adjust to the typed hset/hget
// methods that version documents.
import redis from 'k6/experimental/redis';

const REDIS_URL = __ENV.REDIS_URL || 'redis://127.0.0.1:6379';
const client = new redis.Client(REDIS_URL);

const POOL_KEY = 'pool:active';
const MIN_MAX_DOWNLOADS = 15;
const MAX_MAX_DOWNLOADS = 60;
const CODE_TTL_SECONDS = 3600; // bounds memory over a long run

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// Called by a sender after a successful upload: makes the code available to receivers
// with a random lifetime (in downloads, not time) between 15 and 60 inclusive.
export async function pushCode(fileCode) {
  const maxDownloads = randomInt(MIN_MAX_DOWNLOADS, MAX_MAX_DOWNLOADS);
  const codeKey = `code:${fileCode}`;

  await client.sadd(POOL_KEY, fileCode);
  await client.sendCommand('HSET', codeKey, 'max', String(maxDownloads), 'count', '0');
  await client.expire(codeKey, CODE_TTL_SECONDS);
}

// Called by a receiver: returns a live fileCode to download, or null if the pool is
// currently empty (the caller should back off and retry rather than fail the iteration).
//
// Not wrapped in a Lua transaction: SRANDMEMBER, HINCRBY and the retire check are three
// separate round-trips, so under concurrent receivers a code can occasionally get 1-2
// downloads past its assigned `max` before SREM removes it from the pool. That's an
// accepted, harmless approximation for a benchmark (the exact download count per code
// isn't a correctness requirement here) -- not worth the complexity of a Lua script for.
export async function pullCode() {
  const fileCode = await client.srandmember(POOL_KEY);
  if (!fileCode) {
    return null;
  }

  const codeKey = `code:${fileCode}`;
  const count = Number(await client.sendCommand('HINCRBY', codeKey, 'count', '1'));
  const maxReply = await client.sendCommand('HGET', codeKey, 'max');
  const max = Number(maxReply || MAX_MAX_DOWNLOADS);

  if (count >= max) {
    await client.srem(POOL_KEY, fileCode);
  }

  return fileCode;
}

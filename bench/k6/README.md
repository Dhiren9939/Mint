# bench/k6

A single k6 script (`main.js`) load-testing each bench arm (`bench-sql`, `bench-dynamo`,
`bench-cache`, `bench-ecs`) with a **sender/receiver user model** instead of a dialed
request rate. Run it **from this directory** (`bench/k6/`) -- `open('./fixtures/payload.bin', ...)`
is relative to the script file, so it only resolves correctly when k6 is invoked from here.

```sh
cd bench/k6
k6 run -e BASE_URL=https://mint-bench-ecs.dhiren.xyz \
  -e SENDER_VUS=5 -e RECEIVER_VUS=20 -e DURATION_MINUTES=5 main.js
```

## The model

Two k6 scenarios run **concurrently**, each a closed loop with think-time
(`ramping-vus`, not `ramping-arrival-rate`): offered load is an emergent property of
VU count x cycle time, matching how real users behave, rather than a fixed dialed rate.

- **`senders`** (`exec: 'sender'`): loop --
  1. `POST /api/v1/file/upload` with a payload-size field only (`contentSize` from
     `PAYLOAD_SIZE_KB`, default the size of the checked-in `fixtures/payload.bin`).
  2. `sleep(SIMULATED_UPLOAD_SECONDS)` (default 0.5s) stands in for the S3 PUT.
     **No real S3 traffic happens at all** -- the bytes in `fixtures/payload.bin` are
     never read past `.byteLength`, let alone sent. This isolates the app/DB/cache path
     from S3/network variance, which is what these benchmarks are actually about.
  3. `PATCH /api/v1/file` to confirm.
  4. On success, pushes the new `fileCode` into the shared pool (see below) with a
     random `maxDownloads` between 15 and 60 inclusive.
  5. `sleep(random(SENDER_THINK_MIN, SENDER_THINK_MAX))` (default 2-10s).
  6. Repeat.
- **`receivers`** (`exec: 'receiver'`): loop --
  1. Pulls a live code from the pool. If the pool is momentarily empty (cold start, or
     senders haven't caught up yet), it sleeps 1s and retries on the next iteration --
     this never fails the iteration.
  2. `GET /api/v1/file/{fileCode}`.
  3. `sleep(random(RECEIVER_THINK_MIN, RECEIVER_THINK_MAX))` (default 1-5s).
  4. Repeat.

Both scenarios ramp the same way: 30s up to their peak VU count (`SENDER_VUS` /
`RECEIVER_VUS`), hold for `DURATION_MINUTES`, 15s ramp down to 0.

There is no separate seeding step anymore, and no `seeded-codes.json` file: the pool
starts empty and senders populate it live as the run progresses.

### Why cache hit/miss isn't dialed anymore

The old Zipf/uniform sampling forced a specific hot/cold mix. This model produces the
mix organically instead: a code downloaded quickly (short receiver think-time, still in
the active pool) stays inside the cache's 300s TTL and mostly hits it (thanks to the
Stage 0 fill-on-miss fix); a code that lingers in the pool crosses the TTL and its later
downloads genuinely miss. **Real limitation, stated plainly**: there's no dedicated
"guaranteed cold, long-expired code" scenario anymore -- only whatever mix this
sender/receiver traffic organically produces. That's fine for comparing the four bench
arms against each other (same script, same behavior everywhere), but it is not a
substitute for a dedicated cache-hit-ratio test if one is ever needed later.

## The shared pool (`lib/pool.js`)

k6 does **not** share JS state between VUs -- each VU is an isolated runtime, so a
module-level array/object cannot coordinate "senders push a code, receivers pull a live
code" the way `seed.js` used to use a module array for a single VU. The fix: a small
**local Redis** on the load-gen EC2 instance itself, driven from k6 via the stock,
bundled `k6/experimental/redis` client (no custom xk6 build). This Redis is purely
load-gen coordination state -- **not** the application's own Valkey, and it never
touches the system under test. It's disposable: no persistence, wiped every run.

- `pool:active` (a Set) holds live codes.
- `code:<fileCode>` (a Hash) holds `max` and `count`.
- **Push** (sender, on a successful upload): `SADD pool:active <code>`;
  `HSET code:<code> max <rand 15..60> count 0`; `EXPIRE code:<code> 3600` (bounds memory
  over a long run).
- **Pull** (receiver): `SRANDMEMBER pool:active` -> `HINCRBY code:<code> count 1` -> if
  `count >= max`, `SREM pool:active <code>`. This is **not** wrapped in a Lua
  transaction, so under concurrent receivers a code can occasionally get 1-2 downloads
  past its assigned `max` at the boundary -- accepted as a harmless approximation for a
  benchmark, not a correctness requirement (see the comment in `lib/pool.js`).
- Connects to `redis://127.0.0.1:6379` by default (`REDIS_URL` env override) -- the
  local Redis the load-gen's `user_data.sh` installs, not the app's cache.
- **API-surface caveat**: k6's `k6/experimental/redis` client's typed Set methods
  (`sadd`/`srem`/`srandmember`/`expire`) are stable across recent k6 releases, but the
  Hash operations (`HSET`/`HINCRBY`/`HGET`) go through the client's generic
  `sendCommand(...)` escape hatch instead of typed methods, since their exact typed
  signatures have shifted between k6 versions and this wasn't verified against a locally
  installed k6 binary. Run `k6 version` on the load-gen instance and sanity-check this
  against that version's docs before relying on it for a real run.

## Custom metrics and thresholds

Every request is classified into exactly one of three k6 `Rate` metrics (`lib/metrics.js`,
unchanged from the earlier design):

- `rate_limited` -- HTTP 429. No threshold: a rise here means the estimated
  `mint.cap.*` needs to go higher for this run, not that the run failed.
- `server_errors` -- 5xx, or a request that hit its explicit `timeout: '10s'` (status 0).
- `client_errors` -- any other 4xx (a real script bug).

Thresholds are a blunt "still alive" bound, not a per-arm SLO (the four arms have
honestly different latencies -- that comparison is the dashboard/summary's job):

- `server_errors: rate<0.01`
- `checks: rate>0.99`
- Downloads (`step:download`): `http_req_duration: p(95)<3000, p(99)<6000` (ms)
- Uploads (`step:generate_upload_link` / `step:confirm_upload`, excludes the simulated
  sleep): `p(95)<5000, p(99)<10000` (ms)

A threshold breach does **not** fail `k6 run`'s exit code in a way that should fail CI --
breaching at a high VU count is the expected way to find the knee. `bench-run.yml` runs
k6 with `|| true` and inspects the summary separately, so it never fails the job purely
on a threshold breach; only an actual script/setup error does.

## Env vars

| Var | Default | Meaning |
|---|---|---|
| `BASE_URL` | *(required)* | Target API base, e.g. `https://mint-bench-ecs.dhiren.xyz` |
| `SENDER_VUS` | 5 | Peak sender VU count |
| `RECEIVER_VUS` | 20 | Peak receiver VU count |
| `DURATION_MINUTES` | 5 | Hold time at peak VUs, after the 30s ramp-up (always a 15s ramp-down after) |
| `SENDER_THINK_MIN` / `SENDER_THINK_MAX` | 2 / 10 (seconds) | Sender think-time range between upload cycles |
| `RECEIVER_THINK_MIN` / `RECEIVER_THINK_MAX` | 1 / 5 (seconds) | Receiver think-time range between downloads |
| `SIMULATED_UPLOAD_SECONDS` | 0.5 | Sleep standing in for the (never-performed) S3 PUT |
| `PAYLOAD_SIZE_KB` | size of `fixtures/payload.bin` (1024) | `contentSize` sent to `/api/v1/file/upload` -- no bytes are actually transferred |
| `EXPIRY_DURATION` | `HOURS24` | `expiryDuration` sent to `/api/v1/file/upload` (`MINUTES15`/`MINUTES30`/`MINUTES60`/`HOURS24`) |
| `REDIS_URL` | `redis://127.0.0.1:6379` | The load-gen box's local coordination Redis |

## Rate limiter caps

There's no dialed RPS to derive an exact number from anymore. Set the target arm's
`mint.cap.*` generously above a rough estimate: `~ SENDER_VUS / avg(SENDER_THINK)` and
`~ RECEIVER_VUS / avg(RECEIVER_THINK)` give ballpark POST/GET rates; multiply by 5-10x
headroom. Confirm after each run by checking `mint.ratelimit.decisions{result=rejected}`
on the dashboard -- if it's non-zero and wasn't the thing being tested, raise the caps
and re-run.

## The `MINT_ID` cookie

k6 gives each VU its own `http.CookieJar` by default (`http.cookieJar()` to inspect it),
and that jar persists across iterations/requests of that VU automatically -- confirmed
against k6's documented per-VU cookie handling, not assumed. Since the backend's
`RateLimitFilter` sets `MINT_ID` via `Set-Cookie` on a VU's first request, every
subsequent request from that same VU keeps using the same `MINT_ID`, so the "user"
rate-limit bucket is exercised per-VU exactly like a real browser session would be. No
extra cookie-jar code is needed.

## `fixtures/payload.bin`

A checked-in 1 MiB binary fixture. Only its byte length is ever used (as the
`contentSize` field on the upload request) -- no bytes are read from it beyond that or
sent anywhere, since this model does not perform a real S3 PUT.

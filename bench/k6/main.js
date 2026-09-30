// Sender/receiver user model: two closed-loop k6 scenarios running concurrently, each
// with think-time, rather than a dialed request rate. Offered load is an emergent
// property of VU count x cycle time -- the right model for "users," not a fixed rate.
//
// No real S3 traffic: senders POST /api/v1/file/upload, sleep to stand in for the PUT,
// then PATCH /api/v1/file to confirm. This isolates the app/DB/cache path from S3/network
// variance, which is what these benchmarks are actually about.
//
//   cd bench/k6 && k6 run -e BASE_URL=https://mint-bench-ecs.dhiren.xyz \
//     -e SENDER_VUS=5 -e RECEIVER_VUS=20 -e DURATION_MINUTES=5 main.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { classify, makeOutcomeCounters } from './lib/metrics.js';
import { pushCode, pullCode } from './lib/pool.js';

const BASE_URL = __ENV.BASE_URL;
if (!BASE_URL) {
  throw new Error('BASE_URL env var is required, e.g. -e BASE_URL=https://mint-bench-ecs.dhiren.xyz');
}

const SENDER_VUS = Number(__ENV.SENDER_VUS || 5);
const RECEIVER_VUS = Number(__ENV.RECEIVER_VUS || 20);
const DURATION_MINUTES = Number(__ENV.DURATION_MINUTES || 5);

const SENDER_THINK_MIN = Number(__ENV.SENDER_THINK_MIN || 2);
const SENDER_THINK_MAX = Number(__ENV.SENDER_THINK_MAX || 10);
const RECEIVER_THINK_MIN = Number(__ENV.RECEIVER_THINK_MIN || 1);
const RECEIVER_THINK_MAX = Number(__ENV.RECEIVER_THINK_MAX || 5);
const SIMULATED_UPLOAD_SECONDS = Number(__ENV.SIMULATED_UPLOAD_SECONDS || 0.5);
const EXPIRY_DURATION = __ENV.EXPIRY_DURATION || 'HOURS24';

// open() is relative to this file, so this resolves to bench/k6/fixtures/payload.bin.
// The bytes are never actually sent -- only .byteLength is used, for the contentSize
// field on the upload request (no real S3 PUT happens in this model).
const payloadBytes = open('./fixtures/payload.bin', 'b');
const PAYLOAD_SIZE_KB = Number(__ENV.PAYLOAD_SIZE_KB || Math.round(payloadBytes.byteLength / 1024));

const counters = makeOutcomeCounters();

function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}

function stages(target) {
  return [
    { target, duration: '30s' },
    { target, duration: `${DURATION_MINUTES}m` },
    { target: 0, duration: '15s' },
  ];
}

export const options = {
  scenarios: {
    senders: {
      executor: 'ramping-vus',
      exec: 'sender',
      startVUs: 0,
      stages: stages(SENDER_VUS),
    },
    receivers: {
      executor: 'ramping-vus',
      exec: 'receiver',
      startVUs: 0,
      stages: stages(RECEIVER_VUS),
    },
  },
  thresholds: {
    server_errors: ['rate<0.01'],
    checks: ['rate>0.99'],
    // Same bound for every endpoint (download, generate-upload-link, confirm-upload) --
    // these are real target SLOs, not just a "didn't fall over" sanity check.
    http_req_duration: ['p(95)<200', 'p(99)<600'],
  },
};

export async function sender() {
  const contentSize = PAYLOAD_SIZE_KB * 1024;
  const fileName = `bench-${__VU}-${__ITER}-${Date.now()}.bin`;

  const genRes = http.post(
    `${BASE_URL}/api/v1/file/upload`,
    JSON.stringify({
      expiryDuration: EXPIRY_DURATION,
      fileName,
      contentType: 'application/octet-stream',
      contentSize,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s',
      tags: { step: 'generate_upload_link' },
    }
  );
  classify(genRes, counters);
  const genOk = check(genRes, { 'upload link generated (201)': (r) => r.status === 201 });

  if (genOk) {
    const genData = genRes.json('data');

    // Stands in for the S3 PUT: this model never actually transfers the bytes, so the
    // datastore/rate-limiter path under test is isolated from S3/network variance.
    sleep(SIMULATED_UPLOAD_SECONDS);

    if (genData) {
      const patchRes = http.patch(
        `${BASE_URL}/api/v1/file`,
        JSON.stringify({
          fileKey: genData.fileKey,
          fileCode: genData.fileCode,
        }),
        {
          headers: { 'Content-Type': 'application/json' },
          timeout: '10s',
          tags: { step: 'confirm_upload' },
        }
      );
      classify(patchRes, counters);
      const patchOk = check(patchRes, { 'upload confirmed (200)': (r) => r.status === 200 });

      if (patchOk) {
        await pushCode(genData.fileCode);
      }
    }
  }

  sleep(randomBetween(SENDER_THINK_MIN, SENDER_THINK_MAX));
}

export async function receiver() {
  const fileCode = await pullCode();
  if (!fileCode) {
    // Pool momentarily empty (cold start, or senders haven't caught up) -- back off and
    // retry on the next iteration rather than failing.
    sleep(1);
    return;
  }

  const res = http.get(`${BASE_URL}/api/v1/file/${fileCode}`, {
    timeout: '10s',
    tags: { step: 'download' },
  });
  classify(res, counters);
  check(res, { 'download link generated (200)': (r) => r.status === 200 });

  sleep(randomBetween(RECEIVER_THINK_MIN, RECEIVER_THINK_MAX));
}

// Shared custom metrics + outcome classification for every k6 scenario.
//
// These are k6 Rate metrics (not Counter): each request adds a 1 or 0 to each of the
// three metrics, so the metric's value is a *proportion* of requests, matching the
// `rate<0.01` style thresholds used across scenarios (e.g. "server_errors: rate<0.01").
import { Rate } from 'k6/metrics';

export function makeOutcomeCounters() {
  return {
    rateLimited: new Rate('rate_limited'),
    serverErrors: new Rate('server_errors'),
    clientErrors: new Rate('client_errors'),
  };
}

// Classifies one HTTP response into rate_limited (429) / server_errors (5xx or a request
// that hit its timeout, status 0) / client_errors (any other 4xx, i.e. a script bug) and
// records it against all three Rate metrics (0 for the two that don't match), so each
// metric's rate is "fraction of all requests in this category".
export function classify(res, counters) {
  const isRateLimited = res.status === 429;
  const isServerError = res.status === 0 || res.status >= 500;
  const isClientError = !isRateLimited && res.status >= 400 && res.status < 500;

  counters.rateLimited.add(isRateLimited);
  counters.serverErrors.add(isServerError);
  counters.clientErrors.add(isClientError);

  if (isRateLimited) return 'rate_limited';
  if (isServerError) return 'server_error';
  if (isClientError) return 'client_error';
  return 'ok';
}

-- Three token buckets checked together, all or nothing.
-- KEYS[1..3]  global, ip, user bucket keys (same order as the Java filter)
-- ARGV[1..6]  capacity, window in ms for global, then ip, then user
-- returns 0 if allowed, or 1 / 2 / 3 for the bucket that said no (global / ip / user)

local t = redis.call('TIME')
local now = t[1] * 1000 + math.floor(t[2] / 1000)

local have = {}

for i = 1, 3 do
    local cap = tonumber(ARGV[i * 2 - 1])
    local window = tonumber(ARGV[i * 2])

    local state = redis.call('HMGET', KEYS[i], 'tokens', 'ts')
    local tokens = cap                              -- no key = a full bucket
    if state[1] then
        local elapsed = math.max(0, now - tonumber(state[2]))
        tokens = math.min(cap, tonumber(state[1]) + elapsed * cap / window)   -- greedy refill
    end

    if tokens < 1 then
        return i                                    -- nothing was taken, so nothing to give back
    end
    have[i] = tokens
end

for i = 1, 3 do
    local cap = tonumber(ARGV[i * 2 - 1])
    local window = tonumber(ARGV[i * 2])
    local left = have[i] - 1

    redis.call('HSET', KEYS[i], 'tokens', string.format('%.6f', left), 'ts', now)
    -- once the bucket would be full again the key is the same as no key, so let it expire then
    redis.call('PEXPIRE', KEYS[i], math.ceil((cap - left) / cap * window) + 1000)
end

return 0

-- KEYS: one ZSET key for each applicable limit dimension.
-- ARGV: nowMillis, requestId, then limit/windowMillis pairs in KEYS order.
local now = tonumber(ARGV[1])
local requestId = ARGV[2]
local maxCount = 0
local maxRetryAfter = 0

for i = 1, #KEYS do
    local limit = tonumber(ARGV[3 + (i - 1) * 2])
    local windowMillis = tonumber(ARGV[4 + (i - 1) * 2])
    local key = KEYS[i]
    redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMillis)
    local count = redis.call('ZCARD', key)
    if count > maxCount then
        maxCount = count
    end
    if count >= limit then
        local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
        local retryAfter = windowMillis
        if oldest[2] ~= nil then
            retryAfter = windowMillis - (now - tonumber(oldest[2]))
        end
        if retryAfter > maxRetryAfter then
            maxRetryAfter = retryAfter
        end
        return {0, count, math.max(1, maxRetryAfter)}
    end
end

for i = 1, #KEYS do
    local windowMillis = tonumber(ARGV[4 + (i - 1) * 2])
    redis.call('ZADD', KEYS[i], now, requestId .. ':' .. i)
    redis.call('PEXPIRE', KEYS[i], windowMillis + 1000)
end

return {1, maxCount + 1, 0}

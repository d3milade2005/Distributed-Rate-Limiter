-- KEYS[1] = the redis key e.g. "tenant-abc:user-123:ai_generate"
-- ARGV[1] = window size in milliseconds e.g. 10000
-- ARGV[2] = limit e.g. 5
-- ARGV[3] = current timestamp in milliseconds
-- ARGV[4] = unique request id
-- ARGV[5] = request cost

local key       = KEYS[1]
local window_ms = tonumber(ARGV[1])
local limit     = tonumber(ARGV[2])
local now       = tonumber(ARGV[3])
local req_id    = ARGV[4]
local cost      = tonumber(ARGV[5])

if window_ms == nil or limit == nil or now == nil or req_id == nil or cost == nil then
    error('invalid sliding window arguments')
end

if cost <= 0 then
    error('cost must be greater than zero')
end

-- step 1: remove all entries older than the window
redis.call('ZREMRANGEBYSCORE', key, 0, now - window_ms)

-- step 2: count how many requests are in the window
local count = redis.call('ZCARD', key)

-- step 3: check if under the limit
if count + cost <= limit then
    for i = 1, cost do
        redis.call('ZADD', key, now, req_id .. ':' .. i)
    end
    redis.call('PEXPIRE', key, window_ms)
    return {1, limit - count - cost, 0}
else
    local slots_needed = count + cost - limit
    local reset_at = 0

    if slots_needed > 0 then
        local boundary = redis.call('ZRANGE', key, 0, slots_needed - 1, 'WITHSCORES')
        if #boundary >= 2 then
            reset_at = tonumber(boundary[#boundary]) + window_ms
        end
    end

    return {0, 0, reset_at}
end

-- KEYS[1] = the redis key e.g. "tenant-abc:user-123:ai_generate"
-- ARGV[1] = window size in milliseconds e.g. 10000
-- ARGV[2] = limit e.g. 5
-- ARGV[3] = current timestamp in milliseconds
-- ARGV[4] = unique request id

local key       = KEYS[1]
local window_ms = tonumber(ARGV[1])
local limit     = tonumber(ARGV[2])
local now       = tonumber(ARGV[3])
local req_id    = ARGV[4]

-- step 1: remove all entries older than the window
redis.call('ZREMRANGEBYSCORE', key, 0, now - window_ms)

-- step 2: count how many requests are in the window
local count = redis.call('ZCARD', key)

-- step 3: check if under the limit
if count < limit then
    -- add this request to the sorted set
    -- score = timestamp, member = unique request id
    redis.call('ZADD', key, now, req_id)
    -- reset the expiry on the key
    redis.call('PEXPIRE', key, window_ms)
    -- return allowed=1, remaining slots
    return {1, limit - count - 1, 0}
else
    -- get the oldest entry to calculate when a slot opens up
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local reset_at = 0
    if #oldest > 0 then
        reset_at = tonumber(oldest[2]) + window_ms
    end
    -- return allowed=0, remaining=0, reset_at timestamp
    return {0, 0, reset_at}
end
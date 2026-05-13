-- KEYS[1] = the redis key e.g. "tenant-abc:user-123:ai_generate"
-- ARGV[1] = capacity (max tokens)
-- ARGV[2] = refill_rate (tokens per second)
-- ARGV[3] = current timestamp in milliseconds
-- ARGV[4] = cost (how many tokens this request needs)

local key         = KEYS[1]
local capacity    = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now         = tonumber(ARGV[3])
local cost        = tonumber(ARGV[4])

if capacity == nil or refill_rate == nil or now == nil or cost == nil then
    error('invalid token bucket arguments')
end

if refill_rate <= 0 then
    error('refill_rate must be greater than zero')
end

if cost <= 0 then
    error('cost must be greater than zero')
end

local bucket      = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens      = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- first request - initialize to full bucket
if tokens == nil then
    tokens = capacity
end
if last_refill == nil then
    last_refill = now
end

-- refill based on elapsed time
local elapsed       = (now - last_refill) / 1000.0
local tokens_to_add = elapsed * refill_rate
tokens = math.min(capacity, tokens + tokens_to_add)

-- check if enough tokens
if tokens >= cost then
    tokens = tokens - cost
    redis.call('HSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(now))
    redis.call('PEXPIRE', key, 60000)
    return {1, math.floor(tokens), 0}
else
    local tokens_needed = cost - tokens
    local wait_ms = math.ceil((tokens_needed / refill_rate) * 1000)
    local reset_at = now + wait_ms
    redis.call('HSET', key, 'tokens', tostring(tokens), 'last_refill', tostring(now))
    redis.call('PEXPIRE', key, 60000)
    return {0, 0, reset_at}
end

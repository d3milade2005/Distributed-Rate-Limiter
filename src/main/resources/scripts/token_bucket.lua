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

-- fetch current state from Redis
local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens      = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- first request for this key - initialize bucket to full
if tokens == nil or last_refill == nil then
    tokens      = capacity
    last_refill = now
end

-- calculate how many tokens to add based on elapsed time
local elapsed        = (now - last_refill) / 1000.0  -- convert ms to seconds
local tokens_to_add  = elapsed * refill_rate
tokens = math.min(capacity, tokens + tokens_to_add)

-- check if enough tokens for this request
if tokens >= cost then
    -- deduct tokens and save state
    tokens = tokens - cost
    redis.call('HSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('PEXPIRE', key, 60000)  -- expire after 60 seconds of inactivity
    -- return allowed=1, remaining tokens
    return {1, math.floor(tokens), 0}
else
    -- not enough tokens - calculate when enough tokens will be available
    local tokens_needed = cost - tokens
    local wait_ms = math.ceil((tokens_needed / refill_rate) * 1000)
    local reset_at = now + wait_ms
    -- save current state (update last_refill so refill continues correctly)
    redis.call('HSET', key, 'tokens', tokens, 'last_refill', now)
    redis.call('PEXPIRE', key, 60000)
    -- return allowed=0, remaining=0, reset_at
    return {0, 0, reset_at}
end
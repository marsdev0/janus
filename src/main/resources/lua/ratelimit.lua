-- Multi-level rate limit check; reject if any level exceeds its threshold.
-- KEYS[1]=rl:token:{tokenId} KEYS[2]=rl:model:{model}
-- ARGV[1]=window seconds (60) ARGV[2]=per-level thresholds, comma-separated "100,50"
-- ARGV[3]=now(ms) ARGV[4]=reqId

local window = tonumber(ARGV[1])
local thresholds = ARGV[2]        -- "100,50"
local now = tonumber(ARGV[3])
local reqId = ARGV[4]
local cutoff = now - window * 1000

-- Parse thresholds (Redis Lua 5.1 has no string.split; use gmatch)
local limits = {}
for v in string.gmatch(thresholds, '[^,]+') do
    table.insert(limits, tonumber(v))
end

for i = 1, #KEYS do
    local k = KEYS[i]
    redis.call('ZREMRANGEBYSCORE', k, 0, cutoff)
    local cnt = redis.call('ZCARD', k)
    if cnt >= limits[i] then
        return -i          -- negative = level i exceeded (-1=token, -2=model)
    end
end
-- All levels passed; record this request on each
for i = 1, #KEYS do
    redis.call('ZADD', KEYS[i], now, reqId .. ':' .. i)
    redis.call('EXPIRE', KEYS[i], window)
end
return 0                    -- allow

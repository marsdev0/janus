-- Symmetric settlement adjustment. ARGV[1]=delta (positive = refund, negative = charge-back).
-- INCRBY accepts negative numbers.
-- KEYS[1]=quota:{tokenId}  ARGV[1]=delta
local cur = redis.call('GET', KEYS[1])
if not cur then
    return -2
end
return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))

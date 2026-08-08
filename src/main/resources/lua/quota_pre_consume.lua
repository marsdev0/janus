-- Pre-deduct quota. Returns:
--   -2  : key not initialized
--   -1  : exceeds overdraft (credit) limit
--   else: new balance after reservation
-- KEYS[1]=quota:{tokenId}  ARGV[1]=reserved  ARGV[2]=creditLimit
local cur = redis.call('GET', KEYS[1])
if not cur then
    return -2
end
local newVal = tonumber(cur) - tonumber(ARGV[1])
if newVal < -tonumber(ARGV[2] or 0) then
    return -1
end
redis.call('SET', KEYS[1], tostring(newVal))
return newVal

-- KEYS[1]=quota:{tokenId}  ARGV[1]=reserved  ARGV[2]=creditLimit
-- -2不存在，-1超过透支额度
local cur = redis.call('GET', KEYS[1])
if not cur then
	return -2
end                              -- 未初始化
local newVal = tonumber(cur) - tonumber(ARGV[1])
if newVal < -tonumber(ARGV[2] or 0) then
	return -1
end    -- 超过透支额度
redis.call('SET', KEYS[1], tostring(newVal))
return newVal

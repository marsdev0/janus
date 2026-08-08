-- KEYS[1]=quota:{tokenId}  ARGV[1]=delta (正=退还 负=补扣)
local cur = redis.call('GET', KEYS[1])
if not cur then
	return -2
end
return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))   -- INCRBY 接受负数

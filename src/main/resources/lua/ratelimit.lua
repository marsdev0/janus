-- 多级限流检查，任一超限返回拒绝
-- KEYS[1]=rl:token:{tokenId} KEYS[2]=rl:model:{model}
-- ARGV[1]=窗口大小（60） ARGV[2]=各级阈值逗号分隔 "100,50" ARGV[3]=now(ms) ARGV[4]=reqId

local window = tonumber(ARGV[1])
local thresholds = ARGV[2]        -- "100,50"
local now = tonumber(ARGV[3])
local reqId = ARGV[4]
local cutoff = now - window * 1000

-- 解析阈值（Redis Lua 5.1 无 string.split，用 gmatch）
local limits = {}
for v in string.gmatch(thresholds, '[^,]+') do
	table.insert(limits, tonumber(v))
end

for i = 1, #KEYS do
	local k = KEYS[i]
	redis.call('ZREMRANGEBYSCORE', k, 0, cutoff)
	local cnt = redis.call('ZCARD', k)
	if cnt >= limits[i] then
		return -i          -- 返回负数表示第 i 级超限（-1=Key级 -2=模型级）
	end
end
-- 全部通过，加入各级
for i = 1, #KEYS do
	redis.call('ZADD', KEYS[i], now, reqId .. ':' .. i)
	redis.call('EXPIRE', KEYS[i], window)
end
return 0                    -- 放行

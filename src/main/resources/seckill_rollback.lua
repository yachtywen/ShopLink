-- Roll back Redis reservation when RocketMQ send fails or an unpaid order is cancelled.
local voucherId = ARGV[1]
local userId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

redis.call('incrby', stockKey, 1)
redis.call('srem', orderKey, userId)
return 0

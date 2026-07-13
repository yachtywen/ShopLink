-- 1. Args
-- 1.1 voucher id
local voucherId = ARGV[1]
-- 1.2 user id
local userId = ARGV[2]

-- 2. Redis keys
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 3. Validate stock
local stock = tonumber(redis.call('get', stockKey))
if (stock == nil or stock <= 0) then
    return 1
end

-- 4. Validate one user one order
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 5. Reserve stock and user order qualification
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
return 0

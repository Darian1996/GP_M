package com.darian.jedis;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.UUID;

public class DistributedLock {


    /***
     * 获得锁
     * @param lockName 锁的名称
     * @param acquireTimeout  获得锁的超时时间
     * @param lockTimeout   锁本身的过期时间
     * @return
     */
    public String acquireLock(String lockName, long acquireTimeout, long lockTimeout) {
        System.err.println(Thread.currentThread().getName() + "-->>" + "开始设置锁" + lockName);

        // 保证释放锁的时候是同一个持有锁的人
        String identifier = UUID.randomUUID().toString();
        String lockKey = "lock:" + lockName;
        int lockExpire = (int) (lockTimeout / 1000);
        Jedis jedis = null;

        try {
            jedis = JedisConnectionUtils.getJedis();
            long end = System.currentTimeMillis() + acquireTimeout;
            // 获取锁的限定时间
            while (System.currentTimeMillis() < end) {
                // 设置锁成功
                if (jedis.setnx(lockKey, identifier) == 1) {
                    // 设置超时时间
                    jedis.expire(lockKey, lockExpire);
                    // 获得锁成功
                    return identifier;
                }
                if (jedis.ttl(lockKey) == -1) {
                    jedis.expire(lockKey, lockExpire);
                }
                try {
                    // 等待片刻后进行获取锁的重试
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } finally {
            jedis.close();
        }
        return null;
    }

    /***
     * 基于 lua 脚本实现释放锁
     */
    public boolean releaseLockWithLua(String lockName, String identifier) {
        System.err.println(Thread.currentThread().getName() + "-->>" + lockName + "LUA 脚本==开始释放锁" + identifier);

        Jedis jedis = JedisConnectionUtils.getJedis();
        String lockKey = "lock:" + lockName;
        String lua = "if redis.call(\"get\",KEYS[1])==ARGV[1] " +
                "then return redis.call(\"del\",KEYS[1]) " +
                "else return 0 end";

        Long rs = (Long) jedis.eval(lua, 1, new String[]{lockKey, identifier});
        if (rs.intValue() > 0) {
            return true;
        }
        return false;
    }

    // 释放锁
    public boolean releaseLock(String lockName, String identifier) {
        System.err.println(Thread.currentThread().getName() + "-->>" + lockName + "开始释放锁" + identifier);

        String lockKey = "lock:" + lockName;
        Jedis jedis = null;
        boolean isRelease = false;
        try {
            jedis = JedisConnectionUtils.getJedis();
            while (true) {
                // watch 是跟事务结合在一起的
                jedis.watch(lockKey);
                // 判断是否为同一把锁
                if (identifier.equals(jedis.get(lockKey))) {
                    Transaction transaction = jedis.multi();
                    transaction.del(lockKey);
                    if (transaction.exec().isEmpty()) {
                        continue;
                    }
                    isRelease = true;
                }
                // TODO 可以抛出异常，这里说明不是同一把锁。
                jedis.unwatch();
                break;
            }
        } finally {
            jedis.close();
        }
        return isRelease;
    }
}

package com.example.edu.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 连接工具类
 * 基于 Jedis + 连接池，封装基础读写操作
 */
public class RedisUtil {
    // Redis 服务地址：本地
    private static final String HOST = "localhost";
    // Redis 默认端口
    private static final int PORT = 6379;

    private static JedisPool jedisPool;

    static {
        // 连接池配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);   // 最大连接数
        config.setMaxIdle(5);     // 最大空闲连接
        config.setMinIdle(1);     // 最小空闲连接
        // 初始化连接池（本地 Redis 无密码）
        jedisPool = new JedisPool(config, HOST, PORT, 2000);
    }

    /**
     * 从连接池获取一个 Jedis 连接
     */
    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    /**
     * 写入缓存并设置过期时间
     * @param key     键
     * @param seconds 过期时间（秒）
     * @param value   值
     */
    public static void setex(String key, int seconds, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, seconds, value);
        }
    }

    /**
     * 读取缓存
     */
    public static String get(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.get(key);
        }
    }

    /**
     * 删除缓存（数据更新时调用，保证缓存与数据库一致）
     */
    public static void del(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
        }
    }
}

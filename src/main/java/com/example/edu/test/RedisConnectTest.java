package com.example.edu.test;

import com.example.edu.util.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * Redis 连接验证：跑通即说明 Jedis 依赖 + Redis 服务都正常
 */
public class RedisConnectTest {
    public static void main(String[] args) {
        System.out.println("========== Redis 连接验证 ==========");
        try (Jedis jedis = RedisUtil.getJedis()) {
            // 1. 测试连通性
            String pong = jedis.ping();
            System.out.println("ping 返回：" + pong);

            // 2. 写入一条测试数据
            jedis.set("demo:key", "hello redis");
            // 3. 读出来
            String value = jedis.get("demo:key");
            System.out.println("写入 demo:key = hello redis");
            System.out.println("读取 demo:key = " + value);

            // 4. 删除测试数据
            jedis.del("demo:key");
            System.out.println("已删除测试数据");

            if ("PONG".equals(pong) && "hello redis".equals(value)) {
                System.out.println("\nRedis 配置成功，可正常读写！");
            } else {
                System.out.println("\n读写结果异常，请检查！");
            }
        } catch (Exception e) {
            System.out.println("\nRedis 连接失败，请确认 Redis 服务已启动！");
            e.printStackTrace();
        }
    }
}

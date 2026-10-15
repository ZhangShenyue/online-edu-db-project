package com.example.edu.test;

import com.example.edu.util.DBUtil;
import com.example.edu.util.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams; // 新增导入

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Redis缓存性能对比测试
 * Cache Aside旁路缓存 + Redis互斥锁(Mutex Key)，防止缓存击穿
 */
public class CacheTest {

    // 缓存过期时间 10分钟，单位秒
    private static final int CACHE_TTL = 10 * 60;
    // 互斥锁过期时间 5000ms，防止死锁
    private static final long LOCK_TTL_MS = 5000L;
    // 重试休眠时间
    private static final long SLEEP_MS = 50L;

    public static void main(String[] args) {
        Long testUserId = 1000L;
        int queryTimes = 100;

        System.out.println("========== Redis缓存性能对比测试 ==========");

        // 1. 直接查询数据库视图
        long startDb = System.currentTimeMillis();
        for (int i = 0; i < queryTimes; i++) {
            queryStudentStatsFromDB(testUserId);
        }
        long endDb = System.currentTimeMillis();
        long totalDb = endDb - startDb;
        double avgDb = (double) totalDb / queryTimes;
        System.out.printf("数据库查询%d次，总耗时：%d ms，单次平均：%.2f ms%n", queryTimes, totalDb, avgDb);

        // 2. 先清空对应缓存，再测试带互斥锁的缓存查询
        try (Jedis jedis = RedisUtil.getJedis()) {
            String cacheKey = "student:stats:" + testUserId;
            jedis.del(cacheKey);
        }

        long startCache = System.currentTimeMillis();
        for (int i = 0; i < queryTimes; i++) {
            getStudentStatsWithMutex(testUserId);
        }
        long endCache = System.currentTimeMillis();
        long totalCache = endCache - startCache;
        double avgCache = (double) totalCache / queryTimes;
        double rate = avgDb / avgCache;
        System.out.printf("缓存查询%d次，总耗时：%d ms，单次平均：%.2f ms%n", queryTimes, totalCache, avgCache);
        System.out.printf("性能提升倍数：%.2f 倍%n", rate);

        System.out.println("==== 实验说明 ====");
        System.out.println("1. 采用Cache Aside旁路缓存模式，使用Mutex互斥锁防止缓存击穿");
        System.out.println("2. 缓存设置过期时间，实现最终一致性");
        System.out.println("3. 适用于读多写少的学生统计信息查询场景");
    }

    /**
     * Cache Aside + Redis互斥锁(Mutex Key)，防止缓存击穿
     * @param userId 学生ID
     * @return 学生统计字符串
     */
    public static String getStudentStatsWithMutex(Long userId) {
        String cacheKey = "student:stats:" + userId;
        String mutexKey = "mutex:student:stats:" + userId;

        try (Jedis jedis = RedisUtil.getJedis()) {
            // 1. 查询缓存
            String cacheValue = jedis.get(cacheKey);
            if (cacheValue != null) {
                return cacheValue;
            }

            // ========== 修复这里！使用SetParams ==========
            String lockRes = jedis.set(mutexKey, "locked", SetParams.setParams().nx().px(LOCK_TTL_MS));

            if ("OK".equals(lockRes)) {
                try {
                    // 获取锁成功，查询数据库
                    String dbData = queryStudentStatsFromDB(userId);
                    if (dbData != null) {
                        jedis.setex(cacheKey, CACHE_TTL, dbData);
                    }
                    return dbData;
                } finally {
                    // 释放锁
                    jedis.del(mutexKey);
                }
            } else {
                // 获取锁失败，短暂休眠后重试
                Thread.sleep(SLEEP_MS);
                return getStudentStatsWithMutex(userId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 从MySQL视图 v_student_study_stats 查询学生统计数据
     */
    private static String queryStudentStatsFromDB(Long userId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM v_student_study_stats WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return "user_id:" + rs.getLong("user_id")
                        + ",选课数:" + rs.getLong("course_count")
                        + ",已完成章节:" + rs.getLong("finished_chapter")
                        + ",平均分:" + rs.getDouble("avg_score");
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }
}

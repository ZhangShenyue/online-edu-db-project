package com.example.edu.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.edu.util.DBUtil;
import com.example.edu.util.RedisUtil;

/**
 * Redis缓存优化对照实验
 * 旁路缓存 Cache Aside：优先查询Redis，未命中再查询MySQL视图，回写缓存
 */
public class CacheTest {
    // 测试用户ID
    private static final int TEST_USER_ID = 301;
    // 循环查询次数
    private static final int QUERY_TIMES = 100;
    // 缓存过期时间，单位秒
    private static final int CACHE_EXPIRE = 60;

    public static void main(String[] args) {
        System.out.println("========== Redis缓存性能对比测试 ==========\n");

        // 清空旧缓存，保证第一轮查询回源数据库
        RedisUtil.del("student:stats:" + TEST_USER_ID);

        // 1. 直接查询MySQL视图（无缓存）
        long dbStart = System.currentTimeMillis();
        for (int i = 0; i < QUERY_TIMES; i++) {
            queryFromDB(TEST_USER_ID);
        }
        long dbCost = System.currentTimeMillis() - dbStart;
        System.out.printf("数据库查询%d次，总耗时：%d ms，单次平均：%.2f ms\n\n",
                QUERY_TIMES, dbCost, dbCost * 1.0 / QUERY_TIMES);

        // 2. 走Redis旁路缓存查询
        long cacheStart = System.currentTimeMillis();
        for (int i = 0; i < QUERY_TIMES; i++) {
            queryFromCache(TEST_USER_ID);
        }
        long cacheCost = System.currentTimeMillis() - cacheStart;
        System.out.printf("缓存查询%d次，总耗时：%d ms，单次平均：%.2f ms\n\n",
                QUERY_TIMES, cacheCost, cacheCost * 1.0 / QUERY_TIMES);

        double speedUp = (double) dbCost / cacheCost;
        System.out.printf("性能提升倍数：%.2f 倍\n", speedUp);

        System.out.println("\n==== 实验说明 ====");
        System.out.println("1. 采用Cache Aside旁路缓存模式");
        System.out.println("2. 缓存设置过期时间，实现最终一致性");
        System.out.println("3. 适用于读多写少的学生统计信息查询场景");
    }

    /**
     * 直接查询数据库视图 v_student_study_stats
     */
    private static String queryFromDB(int userId) {
        String res = null;
        String sql = "SELECT * FROM v_student_study_stats WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                res = rs.getInt("course_count") + ","
                        + rs.getInt("finished_chapter") + ","
                        + rs.getDouble("avg_score");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 旁路缓存逻辑：先查Redis，没有则查数据库并写入缓存
     */
    private static String queryFromCache(int userId) {
        String key = "student:stats:" + userId;
        String value = RedisUtil.get(key);
        if (value != null) {
            // 缓存命中，直接返回
            return value;
        }
        // 缓存未命中，回源MySQL
        value = queryFromDB(userId);
        if (value != null) {
            RedisUtil.setex(key, CACHE_EXPIRE, value);
        }
        return value;
    }
}

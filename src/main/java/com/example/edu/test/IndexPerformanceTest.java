package com.example.edu.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.edu.util.DBUtil;

public class IndexPerformanceTest {
    // 测试10次取平均值，减少波动
    private static final int TEST_TIMES = 10;

    public static void main(String[] args) {
        try {
            System.out.println("===== 【无索引】性能基准测试 =====");
            
            // 场景1：按用户ID查所有学习记录
            testQueryByUserId(1000);
            
            // 场景2：按章节ID查所有学习记录
            testQueryByChapterId(500);
            
            // 场景3：用户ID + 章节ID 精准查询
            testQueryByUserAndChapter(1000, 500);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 场景1：按用户ID查询
    private static void testQueryByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM study_record WHERE user_id = ?";
        long totalTime = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < TEST_TIMES; i++) {
                pstmt.setInt(1, userId);
                
                long start = System.currentTimeMillis();
                ResultSet rs = pstmt.executeQuery();
                // 遍历结果，模拟真实读取数据
                while (rs.next()) {
                    rs.getInt("record_id");
                    rs.getInt("duration");
                }
                long end = System.currentTimeMillis();
                
                totalTime += (end - start);
            }
        }
        System.out.println("按用户ID查询，平均耗时：" + (totalTime / TEST_TIMES) + " ms");
    }

    // 场景2：按章节ID查询
    private static void testQueryByChapterId(int chapterId) throws SQLException {
        String sql = "SELECT * FROM study_record WHERE chapter_id = ?";
        long totalTime = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < TEST_TIMES; i++) {
                pstmt.setInt(1, chapterId);
                
                long start = System.currentTimeMillis();
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    rs.getInt("record_id");
                    rs.getInt("duration");
                }
                long end = System.currentTimeMillis();
                
                totalTime += (end - start);
            }
        }
        System.out.println("按章节ID查询，平均耗时：" + (totalTime / TEST_TIMES) + " ms");
    }

    // 场景3：用户ID + 章节ID 组合查询
    private static void testQueryByUserAndChapter(int userId, int chapterId) throws SQLException {
        String sql = "SELECT * FROM study_record WHERE user_id = ? AND chapter_id = ?";
        long totalTime = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < TEST_TIMES; i++) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, chapterId);
                
                long start = System.currentTimeMillis();
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    rs.getInt("record_id");
                }
                long end = System.currentTimeMillis();
                
                totalTime += (end - start);
            }
        }
        System.out.println("按用户+章节查询，平均耗时：" + (totalTime / TEST_TIMES) + " ms");
    }
}


package com.example.edu.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.example.edu.util.DBUtil;

public class TransactionTest {
    public static void main(String[] args) {
        // 测试：提交作业并更新学习状态
        boolean result = submitHomework(100, 500, 85);
        System.out.println("事务执行结果：" + (result ? "成功" : "失败"));
    }

    /**
     * 原子操作：提交作业 + 更新章节学习状态
     * @param userId 用户ID
     * @param chapterId 章节ID
     * @param score 作业分数
     * @return 是否成功
     */
    public static boolean submitHomework(int userId, int chapterId, int score) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            // 关键：关闭自动提交，开启手动事务
            conn.setAutoCommit(false);

            // 操作1：插入作业提交记录
            String insertSql = "INSERT INTO homework_submit (user_id, chapter_id, submit_time, score) VALUES (?, ?, NOW(), ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, chapterId);
                pstmt.setInt(3, score);
                pstmt.executeUpdate();
            }

            // 模拟异常：把下面这行注释去掉，测试事务回滚
            // int i = 1 / 0;

            // 操作2：更新学习记录为已完成
            String updateSql = "UPDATE study_record SET is_finished = 1 WHERE user_id = ? AND chapter_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, chapterId);
                pstmt.executeUpdate();
            }

            // 全部成功：提交事务
            conn.commit();
            return true;

        } catch (Exception e) {
            // 出现任何异常：回滚事务，两个操作都撤销
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            // 恢复自动提交，关闭连接
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

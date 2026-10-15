package com.example.edu.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.edu.util.DBUtil;

public class ConcurrencyTest {
    // 测试用的学习记录ID
    private static final int TEST_RECORD_ID = 1;
    // 每个线程累加次数
    private static final int ADD_TIMES = 100;
    // 乐观锁最大重试次数
    private static final int MAX_RETRY = 10;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 并发控制三种方案对比测试 ==========\n");

        // 1. 无锁版本
        testWithoutLock();

        // 2. 悲观锁版本
        testWithPessimisticLock();

        // 3. 乐观锁版本
        testWithOptimisticLock();

        // 最终对比总结
        System.out.println("\n========== 三种方案对比总结 ==========");
        System.out.println("理论正确值：200（两个线程各累加100次）");
        System.out.println("1. 无锁：出现丢失更新，结果远小于200，数据不一致");
        System.out.println("2. 悲观锁：结果完全正确，串行执行，一致性强，并发度低");
        System.out.println("3. 乐观锁：结果完全正确，读操作无锁，冲突时重试，适合读多写少");
    }

    // ==================== 1. 无锁版本 ====================
    private static void testWithoutLock() throws InterruptedException {
        resetRecord();
        System.out.println("----- 【无锁】测试开始 -----");

        Thread t1 = new Thread(() -> addWithoutLock(), "线程1");
        Thread t2 = new Thread(() -> addWithoutLock(), "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        int result = getDuration();
        System.out.println("无锁最终结果：" + result);
        System.out.println("是否正确：" + (result == 200 ? "是" : "否（丢失更新）") + "\n");
    }

    private static void addWithoutLock() {
        for (int i = 0; i < ADD_TIMES; i++) {
            try (Connection conn = DBUtil.getConnection()) {
                // 读当前值
                String selectSql = "SELECT duration FROM study_record WHERE record_id = ?";
                int current = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, TEST_RECORD_ID);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        current = rs.getInt("duration");
                    }
                }
                // 写回
                String updateSql = "UPDATE study_record SET duration = ? WHERE record_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, current + 1);
                    pstmt.setInt(2, TEST_RECORD_ID);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ==================== 2. 悲观锁版本 ====================
    private static void testWithPessimisticLock() throws InterruptedException {
        resetRecord();
        System.out.println("----- 【悲观锁】测试开始 -----");

        Thread t1 = new Thread(() -> addWithPessimisticLock(), "线程1");
        Thread t2 = new Thread(() -> addWithPessimisticLock(), "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        int result = getDuration();
        System.out.println("悲观锁最终结果：" + result);
        System.out.println("是否正确：" + (result == 200 ? "是" : "否") + "\n");
    }

    private static void addWithPessimisticLock() {
        for (int i = 0; i < ADD_TIMES; i++) {
            Connection conn = null;
            try {
                conn = DBUtil.getConnection();
                conn.setAutoCommit(false);

                // 核心：FOR UPDATE 加行级排他锁，其他事务阻塞等待
                String selectSql = "SELECT duration FROM study_record WHERE record_id = ? FOR UPDATE";
                int current = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                    pstmt.setInt(1, TEST_RECORD_ID);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        current = rs.getInt("duration");
                    }
                }
                // 更新
                String updateSql = "UPDATE study_record SET duration = ? WHERE record_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, current + 1);
                    pstmt.setInt(2, TEST_RECORD_ID);
                    pstmt.executeUpdate();
                }

                conn.commit(); // 提交事务，释放锁
            } catch (SQLException e) {
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            } finally {
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

    // ==================== 3. 乐观锁版本 ====================
    private static void testWithOptimisticLock() throws InterruptedException {
        resetRecord();
        System.out.println("----- 【乐观锁】测试开始 -----");

        Thread t1 = new Thread(() -> addWithOptimisticLock(), "线程1");
        Thread t2 = new Thread(() -> addWithOptimisticLock(), "线程2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        int result = getDuration();
        System.out.println("乐观锁最终结果：" + result);
        System.out.println("是否正确：" + (result == 200 ? "是" : "否") + "\n");
    }

    private static void addWithOptimisticLock() {
        for (int i = 0; i < ADD_TIMES; i++) {
            int retry = 0;
            boolean success = false;

            while (!success && retry < MAX_RETRY) {
                try (Connection conn = DBUtil.getConnection()) {
                    // 1. 读取数据和当前版本号（全程不加锁）
                    String selectSql = "SELECT duration, version FROM study_record WHERE record_id = ?";
                    int current = 0;
                    int oldVersion = 0;
                    try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                        pstmt.setInt(1, TEST_RECORD_ID);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            current = rs.getInt("duration");
                            oldVersion = rs.getInt("version");
                        }
                    }

                    // 2. 更新时校验版本号：只有版本没被修改才更新，同时版本号+1
                    String updateSql = "UPDATE study_record SET duration = ?, version = version + 1 WHERE record_id = ? AND version = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setInt(1, current + 1);
                        pstmt.setInt(2, TEST_RECORD_ID);
                        pstmt.setInt(3, oldVersion);

                        int affectedRows = pstmt.executeUpdate();
                        // 受影响行数=1 说明更新成功；=0 说明版本号变了，冲突了
                        success = affectedRows == 1;
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }

                retry++;
                if (!success) {
                    // 冲突了，稍等一下重试
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    // ==================== 辅助方法 ====================
    /**
     * 重置测试记录：时长归零，版本号归1
     */
    private static void resetRecord() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE study_record SET duration = 0, version = 1 WHERE record_id = ?")) {
            pstmt.setInt(1, TEST_RECORD_ID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询当前时长
     */
    private static int getDuration() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT duration FROM study_record WHERE record_id = ?")) {
            pstmt.setInt(1, TEST_RECORD_ID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("duration");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

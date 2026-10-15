package com.example.edu.generator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.example.edu.util.DBUtil;

public class DataGenerator {
    private static final Random random = new Random();
    // 基准时间：所有数据的时间都在这个基础上随机生成，保证时间逻辑合理
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);

    public static void main(String[] args) {
        try {
            // ===== 基础数据已生成，注释避免重复报错 =====
            // generateUsers(3000);
            // System.out.println("===== 用户数据生成完成 =====");
            
            // generateCourses(100);
            // System.out.println("===== 课程数据生成完成 =====");
            
            // generateChapters();
            // System.out.println("===== 章节数据生成完成 =====");
            
            // ===== 本次运行：生成业务数据 =====
            generateUserCourses();
            System.out.println("===== 选课关系生成完成 =====");
            
            generateStudyRecords();
            System.out.println("===== 学习记录生成完成 =====");
            
            generateHomeworks();
            System.out.println("===== 作业数据生成完成 =====");
            
        } catch (SQLException e) {
            System.out.println("数据生成出错！");
            e.printStackTrace();
        }
    }

    /**
     * 生成用户数据
     * @param count 生成数量
     */
    private static void generateUsers(int count) throws SQLException {
        String sql = "INSERT INTO user_info (username, register_time, user_type) VALUES (?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= count; i++) {
                pstmt.setString(1, "user_" + i);
                LocalDateTime registerTime = BASE_TIME.plusDays(random.nextInt(365));
                pstmt.setObject(2, registerTime);
                pstmt.setString(3, i <= count * 0.1 ? "教师" : "学生");
                pstmt.addBatch();
                
                if (i % 500 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    /**
     * 生成课程数据
     * @param count 课程数量
     */
    private static void generateCourses(int count) throws SQLException {
        List<Integer> teacherIds = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM user_info WHERE user_type = '教师'");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                teacherIds.add(rs.getInt("user_id"));
            }
        }

        String sql = "INSERT INTO course (course_name, teacher_id, publish_time) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= count; i++) {
                pstmt.setString(1, "课程_" + i);
                pstmt.setInt(2, teacherIds.get(random.nextInt(teacherIds.size())));
                LocalDateTime publishTime = BASE_TIME.plusDays(random.nextInt(180));
                pstmt.setObject(3, publishTime);

                pstmt.addBatch();
                if (i % 50 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    /**
     * 生成章节数据：每门课程15个章节
     */
    private static void generateChapters() throws SQLException {
        List<Integer> courseIds = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT course_id FROM course");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                courseIds.add(rs.getInt("course_id"));
            }
        }

        String sql = "INSERT INTO chapter (course_id, chapter_name, chapter_order) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int total = 0;
            for (Integer courseId : courseIds) {
                for (int i = 1; i <= 15; i++) {
                    pstmt.setInt(1, courseId);
                    pstmt.setString(2, "第" + i + "章");
                    pstmt.setInt(3, i);
                    pstmt.addBatch();
                    total++;
                }
            }
            pstmt.executeBatch();
            System.out.println("共生成章节：" + total + " 个");
        }
    }

    /**
     * 生成选课关系：学生-课程多对多
     */
    private static void generateUserCourses() throws SQLException {
        List<Integer> studentIds = new ArrayList<>();
        List<Integer> courseIds = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection()) {
            // 查询所有学生ID
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM user_info WHERE user_type = '学生'");
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    studentIds.add(rs.getInt("user_id"));
                }
            }
            // 查询所有课程ID
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT course_id FROM course");
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courseIds.add(rs.getInt("course_id"));
                }
            }
        }

        String sql = "INSERT INTO user_course (user_id, course_id, enroll_time) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int total = 0;
            for (Integer studentId : studentIds) {
                // 每个学生随机选 5~15 门课
                int courseNum = 5 + random.nextInt(11);
                Set<Integer> selectedCourses = new HashSet<>();
                
                // 去重，避免同一学生重复选同一门课
                while (selectedCourses.size() < courseNum) {
                    selectedCourses.add(courseIds.get(random.nextInt(courseIds.size())));
                }

                for (Integer courseId : selectedCourses) {
                    pstmt.setInt(1, studentId);
                    pstmt.setInt(2, courseId);
                    // 选课时间在基准时间后半年到一年之间
                    LocalDateTime enrollTime = BASE_TIME.plusDays(180 + random.nextInt(180));
                    pstmt.setObject(3, enrollTime);
                    pstmt.addBatch();
                    total++;
                    
                    if (total % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
            }
            pstmt.executeBatch();
            System.out.println("共生成选课记录：" + total + " 条");
        }
    }

    /**
     * 生成学习进度记录
     */
    private static void generateStudyRecords() throws SQLException {
        // 存储选课信息：用户ID、课程ID、选课时间
        List<Object[]> enrollments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT user_id, course_id, enroll_time FROM user_course");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                enrollments.add(new Object[]{
                    rs.getInt("user_id"),
                    rs.getInt("course_id"),
                    rs.getTimestamp("enroll_time").toLocalDateTime()
                });
            }
        }

        // 预存每门课对应的章节ID列表
        java.util.Map<Integer, List<Integer>> courseChapters = new java.util.HashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT course_id, chapter_id FROM chapter ORDER BY course_id");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int courseId = rs.getInt("course_id");
                int chapterId = rs.getInt("chapter_id");
                courseChapters.computeIfAbsent(courseId, k -> new ArrayList<>()).add(chapterId);
            }
        }

        String sql = "INSERT INTO study_record (user_id, chapter_id, start_time, duration, is_finished) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int total = 0;
            for (Object[] enroll : enrollments) {
                int userId = (int) enroll[0];
                int courseId = (int) enroll[1];
                LocalDateTime enrollTime = (LocalDateTime) enroll[2];
                
                List<Integer> chapters = courseChapters.get(courseId);
                for (Integer chapterId : chapters) {
                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, chapterId);
                    // 开始时间在选课时间之后
                    LocalDateTime startTime = enrollTime.plusDays(random.nextInt(60));
                    pstmt.setObject(3, startTime);
                    // 学习时长 10~120 分钟
                    pstmt.setInt(4, 10 + random.nextInt(111));
                    // 80% 概率完成
                    pstmt.setInt(5, random.nextDouble() < 0.8 ? 1 : 0);
                    
                    pstmt.addBatch();
                    total++;
                    
                    if (total % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
            }
            pstmt.executeBatch();
            System.out.println("共生成学习记录：" + total + " 条");
        }
    }

    /**
     * 生成作业提交记录
     */
    private static void generateHomeworks() throws SQLException {
        String sql = "INSERT INTO homework_submit (user_id, chapter_id, submit_time, score) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement queryStmt = conn.prepareStatement(
                 "SELECT user_id, chapter_id, start_time FROM study_record WHERE is_finished = 1")) {
            
            ResultSet rs = queryStmt.executeQuery();
            int total = 0;
            
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                int chapterId = rs.getInt("chapter_id");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                
                pstmt.setInt(1, userId);
                pstmt.setInt(2, chapterId);
                // 提交时间在学习开始时间之后
                LocalDateTime submitTime = startTime.plusHours(1 + random.nextInt(48));
                pstmt.setObject(3, submitTime);
                // 分数 40~100
                pstmt.setInt(4, 40 + random.nextInt(61));
                
                pstmt.addBatch();
                total++;
                
                if (total % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            System.out.println("共生成作业记录：" + total + " 条");
        }
    }

    /**
     * 工具方法：生成指定范围内的随机时间
     */
    private static LocalDateTime randomTime(LocalDateTime start, int daysRange) {
        return start.plusDays(random.nextInt(daysRange))
                .plusHours(random.nextInt(24))
                .plusMinutes(random.nextInt(60));
    }
}

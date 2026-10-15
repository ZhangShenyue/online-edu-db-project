package com.example.edu.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class DBUtil {
    // 数据库连接参数
	private static final String URL = "jdbc:mysql://localhost:3306/edu_db?useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "zhang20060601";
    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 关闭连接
    public static void close(Connection conn) {
        try {
            if(conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 重载：关闭 ResultSet、PreparedStatement、Connection（新增，给CacheTest使用）
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if(rs != null) rs.close();
            if(pstmt != null) pstmt.close();
            if(conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 测试连接
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            System.out.println("数据库连接成功！");
            close(conn);
        } catch (SQLException e) {
            System.out.println("数据库连接失败！");
            e.printStackTrace();
        }
    }
}

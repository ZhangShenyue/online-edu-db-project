package com.example.edu.util;

import java.sql.Connection;
import java.sql.DriverManager;
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

    // 测试连接
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            System.out.println("数据库连接成功！");
            conn.close();
        } catch (SQLException e) {
            System.out.println("数据库连接失败！");
            e.printStackTrace();
        }
    }
}

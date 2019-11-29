package com.gty.util;

import com.google.common.base.Joiner;
import com.gty.domain.XCell;
import com.gty.domain.XRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {

    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static String ip = "127.0.0.1";
    private static String port = "3306";
    private static String db = "dev";
    private static String name = "root";
    private static String password = "112233";
    private static String tableName = "tableName";

    private static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(generateDBUrl(ip, port, db), name, password);
        } catch (ClassNotFoundException e) {
            System.out.println("can not load jdbc driver" + e);
        } catch (SQLException e) {
            System.out.println("get connection failure" + e);
        }
        return conn;
    }

    /**
     * 生成一个连接url
     * @param ip     地址
     * @param port   端口
     * @param dbname 数据库名
     * @return
     */
    private static String generateDBUrl(String ip, String port, String dbname) {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:mysql://").append(ip);
        sb.append(":").append(port);
        sb.append("/").append(dbname);
        //sb.append("?useUnicode=true&amp;characterEncoding=utf-8&amp;autoreconnect=true&useSSL=false");
        sb.append("?useUnicode=true&characterEncoding=UTF-8&autoreconnect=true&useSSL=false&rewriteBatchedStatements=true&serverTimezone=GMT%2B8");
        return sb.toString();
    }

    /**
     * 关闭数据库连接
     * @param conn
     */
    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 1.创建数据库表
     * @param xRow 表格的列名作为数据库的列名的注释
     * @return 返回一个数据库的列名 column_x
     */
    public static List<String> createTable(XRow xRow) {
        Connection conn = getConnection();
        List<XCell> columnList = xRow.getRowValue();
        Statement stmt;
        List<String> dbColumnList = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(tableName).append("(");
            sb.append("id BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY, ");
            int index = 0;
            for (XCell colimn : columnList) {
                index++;
                dbColumnList.add("column" + index);
                sb.append("column" + index + " ");
                sb.append("varchar(200) null ");
                if (index < columnList.size()) {
                    sb.append("COMMENT '" + colimn.getValue() + "', ");
                } else {
                    sb.append("COMMENT '" + colimn.getValue() + "'");
                }
            }
            sb.append(") ENGINE = InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;");
            stmt = conn.createStatement();
            if (0 == stmt.executeLargeUpdate(sb.toString())) {
                System.out.println("成功创建表！");
            } else {
                System.out.println("创建表失败！");
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        //返回的是当前的列.
        return dbColumnList;
    }

    /**
     * 2.生成一条插入数据的SQL语句
     * @param columnList 数据库的列名,从createTable方法中获取
     * @return 返回一个SQL语句
     */
    private static String generateInsertSQL(List<String> columnList) {
        List<String> columnDataList = new ArrayList<>();
        for (int i = 0; i < columnList.size(); i++) {
            columnDataList.add("?");
        }
        String columnNameStr = Joiner.on(",").join(columnList);
        String columnDataStr = Joiner.on(",").join(columnDataList);
        StringBuilder sb = new StringBuilder();
        sb.append("insert into ");
        sb.append(tableName);
        sb.append(" (");
        sb.append(columnNameStr);
        sb.append(") values(");
        sb.append(columnDataStr);
        sb.append(")");
        return sb.toString();
    }

    /**
     * 插入一个SQL
     *
     * @param columnList 列名
     * @param row        行数据封装类
     * @return
     */
    public static int insertRow(List<String> columnList, XRow row) {
        String insertSQL = generateInsertSQL(columnList);
        int a = 0;
        Connection conn = getConnection();
        try {
            PreparedStatement pst = conn.prepareStatement(insertSQL);
            if (row != null) {
                List<XCell> rowValue = row.getRowValue();
                for (int i = 0; i < rowValue.size(); i++) {
                    XCell xCell = rowValue.get(i);
                    pst.setString(i + 1, xCell.getValue());
                }
            }
            a = pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return a;
    }

    /**
     * 3.批量插入SQL语句
     * @param columnList 列名
     * @param otherData  批量行数据
     * @return
     */
    public static int[] insertRowBatch(List<String> columnList, List<XRow> otherData) {
        String insertSQL = generateInsertSQL(columnList);
        int[] a = null;
        Connection conn = getConnection();

        try {
            PreparedStatement pst = conn.prepareStatement(insertSQL);
            for (XRow rows : otherData) {
                List<XCell> rowValue = rows.getRowValue();
                for (int i = 0; i < rowValue.size(); i++) {
                    XCell xCell = rowValue.get(i);
                    pst.setString(i + 1, xCell.getValue());
                }
                pst.addBatch();
            }
            long startTime = System.currentTimeMillis();
            a = pst.executeBatch();
            long endTime = System.currentTimeMillis();
            System.out.println("插入用时" + (endTime - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        //返回-2是执行成功
        return a;
    }

    /**
     * 4.获取数据库下的所有表名
     */
    public static List<String> getDbAllTables() {
        /*t_roles  t_user  tablename  tablename1  uf_1122  sys_config*/
        List<String> tableNames = new ArrayList<>();
        Connection conn = getConnection();
        ResultSet rs = null;
        try {
            //获取数据库的元数据
            DatabaseMetaData db = conn.getMetaData();
            //从元数据中获取到所有的表名
            rs = db.getTables(null, null, null, new String[]{"TABLE"});
            while (rs.next()) {
                tableNames.add(rs.getString(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tableNames;
    }
}


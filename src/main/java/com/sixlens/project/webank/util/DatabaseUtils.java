package com.sixlens.project.webank.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @ClassName: DBUtils
 * @Description: //TODO 对 172.20.2.129 mysql 中 dw 数据库相关操作
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */
public class DatabaseUtils {

    // 日志打印
    private static Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

    public static void main(String[] args) throws SQLException {

        System.out.println(getConnection());

        List<String> tablesToExport = getTablesToExport();

        for (String table : tablesToExport) {
            System.out.println(table);
            ifTableExist(table);
        }

    }

    private static DataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            InputStream is = DatabaseUtils.class.getClassLoader().getResourceAsStream("dbcp.properties");
            props.load(is);

            // 若此处使用druid连接池技术，存在bug
            dataSource = BasicDataSourceFactory.createDataSource(props);
        } catch (Exception e) {
            logger.error("创建 dbcp 数据池失败， 报错信息为： {}", e);
            e.printStackTrace();
        }
    }


    /**
     * @Description //TODO 获取数据库连接
     * @Author cwy
     * @Date 2023/6/8 0008
     * @Param []
     * @return java.sql.Connection
     **/
    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            // 日志打印
            logger.error("获取数据库连接失败，报错信息为 {}。", e);
        }
        return null;
    }


    /**
     * @Description //TODO 扫描日志表，获取三个表 bank_status 为null的表名
     * @Author cwy
     * @Date 2023/6/8 0008
     * @Param []
     * @return java.util.List<java.lang.String>
     **/
    public static List<String> getTablesToExport() {

        List<String> tablesToExport = new ArrayList<>();

        String sql = "SELECT table_name FROM tmp_cwy_table_export_log WHERE bank_status IS NULL ;";

        Connection conn = getConnection();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                tablesToExport.add(rs.getString("table_name"));
            }

            tablesToExport = tablesToExport.stream().filter(table -> {
                // 目前只提供该三张表
                return table.matches("tmp_cwy_dwm_org_company_industry_hotfield") ||
                        table.matches("dwm_org_company_industry_hotfield") ||
                        table.matches("dwm_org_company_industry_ipc_loc") ||
                        table.matches("dwm_org_cn_sei");

            }).collect(Collectors.toList());

        } catch (Exception e) {
            // e.printStackTrace();
            logger.error("错误sql {} {}。", sql, e);
        } finally {
            release(conn, ps, rs);
        }
        return tablesToExport;
    }

    /**
     * @Description //TODO 上传完毕，修改日志表对应的表bank_status字段，以标识完成
     * @Author cwy
     * @Date 2023/6/8 0008
     * @Param [table]
     * @return void
     **/
    public static void updateExportLogTaskStatus(String tableName) {
        String sql = StrUtil.format("UPDATE tmp_cwy_table_export_log SET bank_status='1', " +
                        "bank_time=current_timestamp, bank_batch={} WHERE table_name='{}' ",
                DateUtil.format(new Date(), "yyyyMMdd"),
                tableName
        );
        execute(sql);
    }

    @SneakyThrows
    private static void execute(String sql) {
        Connection conn = getConnection();
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.execute();
        } catch (SQLException e) {
            // e.printStackTrace();
            logger.info("sql语句: {} 执行失败，报错信息为： {}", sql, e);
        } finally {
            release(conn, ps);
        }
    }


    /**
     * @return boolean
     * @Description //TODO 判断表是否真的存在
     * @Author cwy
     * @Date 2023/6/8 0008
     * @Param [tableName]
     **/
    public static boolean ifTableExist(String tableName) {

        Connection conn = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;

        long cnt = 0;
        try {
            String sql = StrUtil.format("SELECT COUNT(1) AS count FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='dw' AND TABLE_NAME = '{}' ;", tableName);
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                cnt = rs.getLong("count");
            }
        } catch (Exception e) {
            // e.printStackTrace();
            logger.info("表 {} (129)Mysql的dw数据库中并不存在，报错信息为： {}", tableName, e);
        } finally {
            release(conn, ps, rs);
        }

        return cnt == 1;
    }


    /**
     * @Description //TODO 释放 mysql 资源，避免资源占用浪费，与下构成方法重载
     * @Author cwy
     * @Date 2023/6/8 0008
     * @Param [conn, stmt]
     * @return void
     **/
    public static void release(Connection conn, Statement stmt) {

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 打印日志
            logger.error("异常信息为：" + e);
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // 打印日志
            logger.error("异常信息为：" + e);
        }

    }


    public static void release(Connection conn, Statement stmt, ResultSet rs) {

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // 日志打印
            logger.error("异常信息为：" + e);
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // 日志打印
            logger.error("异常信息为：" + e);
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // 日志打印
            logger.error("异常信息为：" + e);
        }

    }

}

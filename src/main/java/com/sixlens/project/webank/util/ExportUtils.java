package com.sixlens.project.webank.util;

import com.sixlens.project.webank.config.WebankConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName: ExportUtils
 * @Description: //TODO
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */
public class ExportUtils {

    private static Logger logger = LoggerFactory.getLogger(ExportUtils.class);

    // 行内字段分割符
    private static final String FIELD_SEPARATOR = "\u0001";
    // 行分割符
    private static final String LINE_SEPARATOR = "\n";

    // 分页查询的每页大小
    private static final int PAGE_SIZE = 200000;


    /**
     * @Description: 将指定的数据库表导出为 textfile 文件，采用分页技术防止内存溢出
     * @Author: cwy
     * @Date: 2023/6/13 0013
     * @Param: [tableName, currentDate]
     * @return: java.io.File
     **/
    public static File exportTableToTextFile(String tableName, String currentDate) {

        // 获取数据库连接
        Connection conn = DatabaseUtils.getConnection();

        // 创建文件输出目录
        String directoryPath = WebankConfig.LINUX_DATA_PATH + File.separator + currentDate;

        // 若目录不存在，则直接创建目录
        Path path = Paths.get(directoryPath);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            // e.printStackTrace();
            logger.error("操作目录，报错信息为：{}", e);
        }

        // 创建输出文件
        File outputFile = new File(directoryPath + File.separator + tableName + ".full.textfile");

        // 判断文件是否已经存在，如果存在则删除
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // 筛选需要的表字段
        String fieldsString = "id";
        String dwmOrgCompanyIndustryHotfieldStr = String.join(",", Arrays.asList("org_id", "org_name", "hotfield_code", "hotfield_name", "hotfield_pr_level"));
        String dwmOrgCompanyIndustryIpcLocStr  = String.join(",", Arrays.asList("org_id", "org_name", "ipc_group_loc_class",
                "ipc_group_loc_class_name_std", "org_ipc_loc_appl_pat_count", "ipc_loc_org_count", "ipc_loc_pat_count",
                "org_ipc_loc_appl_pat_count_rank", "org_ipc_loc_appl_pat_count_rank_total", "org_ipc_loc_appl_pat_count_ratio"));
        String dwmOrgCnSeiStr = String.join(",", Arrays.asList("org_id", "org_name", "sei_level", "sei_code", "sei_name",
                "sei_public_pat_count", "sei_pr_level"));

        if ("pj_webank_dwm_org_cn_sei".equals(tableName)) {
            fieldsString = dwmOrgCnSeiStr;
        } else if ("pj_webank_dwm_org_company_industry_hotfield".equals(tableName)) {
            fieldsString = dwmOrgCompanyIndustryHotfieldStr;
        } else if ("pj_webank_dwm_org_company_industry_ipc_loc".equals(tableName)) {
            fieldsString = dwmOrgCompanyIndustryIpcLocStr;
        }

        FileWriter fw = null;
        BufferedWriter bw = null;

        try {
            // 创建文件写入对象
            fw = new FileWriter(outputFile);
            bw = new BufferedWriter(fw);

            int pageIndex = 0;
            boolean hasMoreDate = true;
            while (hasMoreDate) {
                // 构造分页查询SQL

                String sql = "SELECT " + fieldsString + " FROM `" + tableName + "` LIMIT ?,?;";
                PreparedStatement ps = null;
                ResultSet rs = null;

                try {
                    // 执行分页查询
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, pageIndex * PAGE_SIZE);
                    ps.setInt(2, PAGE_SIZE);
                    rs = ps.executeQuery();

                    // 获取列数
                    int columnCount = rs.getMetaData().getColumnCount();
                    if (pageIndex == 0) {
                        // 写入表头
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) {
                                bw.write(FIELD_SEPARATOR);
                            }
                            bw.write(rs.getMetaData().getColumnName(i));
                        }
                        bw.write(LINE_SEPARATOR);
                    }

                    // 写入数据
                    boolean hasDataRow = false;
                    while (rs.next()) {
                        hasDataRow = true;

                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) {
                                bw.write(FIELD_SEPARATOR);
                            }
                            String value = rs.getString(i);
                            if (value != null) {
                                // 处理特殊字符
                                value = value.replaceAll("\r", "\\r")
                                        .replaceAll("\n", "\\n")
                                        .replaceAll("\u0001", "\\u0001");
                                bw.write(value);
                            }
                        }
                        bw.write(LINE_SEPARATOR);
                    }

                    if (!hasDataRow) {
                        hasMoreDate = false;
                    }

                } catch (Exception e) {
                    logger.error("读取(129)mysql的dw数据库中表 {} 报错，报错信息为：{} ", tableName, e);
                } finally {
                    // 释放资源
                    DatabaseUtils.release(null, ps, rs);
                }
                pageIndex++;
            }

        } catch (IOException e) {
            logger.error("读取(129)mysql的dw数据库中表 {} 报错，报错信息为：{} ", tableName, e);
        } finally {
            // 关闭文件写入对象
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 释放数据库资源
            DatabaseUtils.release(conn, null, null);
        }
        // 返回生成的文件
        return outputFile;
    }

}

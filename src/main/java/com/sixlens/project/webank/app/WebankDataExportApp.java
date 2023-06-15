package com.sixlens.project.webank.app;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.sixlens.project.webank.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: WebankDataExportApp
 * @Description: //TODO 微众银行数据提供启动类
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */
public class WebankDataExportApp {

    // 日志打印
    private static Logger logger = LoggerFactory.getLogger(WebankDataExportApp.class);


    public static void main(String[] args) throws Exception {

        Date date = new Date();
        String batchDate = DateUtil.format(date, "yyyyMMdd");

        System.out.println(batchDate);

        // 获取到需要处理的表
        List<String> tablesToExport = DatabaseUtils.getTablesToExport();

        for (String table : tablesToExport) {
            System.out.println(table);
        }

        System.out.println();


        if (tablesToExport.size() == 0) {
            logger.info("没有需要处理的表");
        } else {

            File[] sourceFiles = null;

            for (String tableName : tablesToExport) {
                // 判断表是否真实存在
                if (DatabaseUtils.ifTableExist(tableName)) {

                    // 日志记录，记录到底是哪张表被处理
                    logger.info("被处理的表为： {} ", tableName);
                    // 数据转换导出 OceanBase(129) -> Linux(138) 按照微众企同⼤数据外部数据源对接规范V1要求转换成textfile格式，原始文件保留一份
                    File tableFile = ExportUtils.exportTableToTextFile(tableName, batchDate);

                    // 数据加密，按照微众企同⼤数据外部数据源对接规范V1要求加密
                    String encryptedFilePath = tableFile.getParent() + File.separator + "encrypted_" + tableFile.getName();

                    File encryptedTableFile = EncryptUtils.encryptFile(tableFile.getAbsolutePath(), encryptedFilePath);
                    logger.info("被加密的表为： {} ", tableName);

                    DatabaseUtils.updateExportLogTaskStatus(tableName);
                    logger.info("表 {} 日志表 bank_status 字段置为“1”.", tableName);


                    sourceFiles = new File[]{encryptedTableFile};
                } else {
                    // 日志记录
                    logger.warn("表 {} 并不真实存在于(129) Mysql 的 dw 数据库", tableName);
                }
            }

            // 数据压缩，按照微众企同⼤数据外部数据源对接规范V1要求压缩
            String compressedFileName = "data." + batchDate + ".pkg.tar.gz";

            CompressUtils.compressFiles(sourceFiles, compressedFileName);

            File compressedTableFile = new File(compressedFileName);

            // 数据上传至sftp服务器，按照微众企同⼤数据外部数据源对接规范V1要求上传
            String remotePath = "/files/" + batchDate;
            SftpUtils.uploadFile(compressedTableFile, remotePath);

            // 生成空文件，文件完整性校验机制:六棱镜生成{数据包名}.{yyyymmdd}.finish的空文件以标识文件处理完毕；
            String finishFileName = "data." + batchDate + ".finish";
            File finishFile = FileUtil.touch(finishFileName);
            SftpUtils.uploadFile(finishFile, remotePath);
            logger.info("批次 {} 上传至sftp成功", batchDate);

            // 邮件提醒
            EmailUtils.sendNotificationEmail(batchDate);
            logger.info("邮件发送成功");
        }

    }

}

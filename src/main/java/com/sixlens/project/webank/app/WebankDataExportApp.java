package com.sixlens.project.webank.app;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.sixlens.project.webank.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

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


        // 获取到需要处理的表
        List<String> tablesToExport = DatabaseUtils.getTablesToExport();

//        for (String table : tablesToExport) {
//            System.out.println(table);
//        }


        // int numberOfThreads = tablesToExport.size() > 0 ? tablesToExport.size() : 1;
        int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Future<File>> futureList = new ArrayList<>();

        if (tablesToExport.size() == 0) {
            logger.info("没有需要处理的表");
        } else {

            for (String tableName : tablesToExport) {
                futureList.add(executorService.submit(new TableProcessor(tableName, batchDate)));
            }
        }


        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                logger.warn("线程池未正常关闭");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            logger.warn("线程池关闭异常", e);
        }


        List<File> encryptedFiles = new ArrayList<>();
        for (Future<File> future : futureList) {
            try {
                encryptedFiles.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("获取加密文件时出现异常", e);
            }
        }

        String compressedFileName = "/data/cwy/" + batchDate + "/data." + batchDate + ".pkg.tar.gz";
        CompressUtils.compressFiles(encryptedFiles.toArray(new File[0]), compressedFileName);

        File compressedTableFile = new File(compressedFileName);
        String remotePath = "/files/" + batchDate;

        SftpUtils.uploadFile(compressedTableFile, remotePath);
        logger.info("批次 {} 上传至sftp成功", batchDate);

        String finishFileName = "data." + batchDate + ".finish";
        File finishFile = FileUtil.touch(finishFileName);
        SftpUtils.uploadFile(finishFile, remotePath);

        EmailUtils.sendNotificationEmail(batchDate);
        logger.info("提醒邮件发送成功");
    }

    static class TableProcessor implements Callable<File> {

        private String tableName;
        private String batchDate;

        TableProcessor(String tableName, String batchDate) {
            this.tableName = tableName;
            this.batchDate = batchDate;
        }


        /**
         * @Description //TODO 将这些加密文件收集到一个列表中
         * @Author cwy
         * @Date 2023/6/15 0015
         * @Param
         * @return java.io.File
         **/
        @Override
        public File call() throws Exception {

            File encryptedTableFile = null;
            try {
                if (DatabaseUtils.ifTableExist(tableName)) {
                    logger.info("被处理的表为： {}", tableName);
                    File tableFile = ExportUtils.exportTableToTextFile(tableName, batchDate);

                    String encryptedFilePath = tableFile.getParent() + File.separator + "encrypted_" + tableFile.getName();
                    encryptedTableFile = EncryptUtils.encryptFile(tableFile.getAbsolutePath(), encryptedFilePath);
                    logger.info("被加密的表为： {}", tableName);

                    DatabaseUtils.updateExportLogTaskStatus(tableName);
                    logger.info("表 {} 日志表 bank_status 字段置为“1”.", tableName);
                } else {
                    logger.warn("表 {} 并不真实存在于(129) Mysql 的dw数据库", tableName);
                }
            } catch (Exception e) {
                logger.error("处理表 {} 时出现异常", tableName, e);
            }
            return encryptedTableFile;
        }
    }

}

package com.sixlens.project.webank.app;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sixlens.project.webank.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    // 邮件主题和正文
    private static final String EMAIL_SUBJECT = "六棱镜大数据提供数据完成通知";
    private static final String EMAIL_BODY_TEMPLATE = "尊敬的微众银行相关负责人，\n\n" +
            "本月数据源对接已经完成，以下是本次对接的数据包信息：\n\n" +
            "数据包名：%s\n" +
            "数据日期：%s\n" +
            "数据包内包含的数据文件：\n%s\n\n" +
            "具体数据包交付方式请参考微众企同大数据外部数据源对接规范V1。\n\n" +
            "如有疑问，请及时与我们联系。\n\n" +
            "谢谢！\n\n" +
            "此邮件是自动发送，请勿回复。";

    public static void main(String[] args) {

        LocalDate currentDate = LocalDate.now();
        // 生成 T-1 日期
        LocalDate previousDay = currentDate.minusDays(1);
        String batchDate = previousDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));


        String compressedFileName = "/data/cwy/webank/" + batchDate + "/data." + batchDate + ".pkg.tar.gz";
        // System.out.println(compressedFileName);


        // 获取到需要处理的表
        List<String> tablesToExport = DatabaseUtils.getTablesToExport();

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

        boolean isTerminated = false;
        while (!isTerminated) {
            try {
                isTerminated = executorService.awaitTermination(30, TimeUnit.MINUTES);
                if (!isTerminated) {
                    logger.info("线程池尚未关闭，等待更长时间");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                logger.warn("线程池关闭异常", e);
            }
        }

        List<File> encryptedFiles = new ArrayList<>();
        for (Future<File> future : futureList) {
            try {
                encryptedFiles.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("获取加密文件时出现异常", e);
            }
        }

        // CompressUtils.compressFiles(encryptedFiles.toArray(new File[0]), compressedFileName);
        CompressUtils.compressFiles(batchDate, compressedFileName); // 之前转换、加密操作都正常执行完成，可直接使用该方法

        // 将压缩后的文件切割成1.8G大小的文件，待实现逻辑
        List<File> splitFiles = CompressUtils.splitCompressedFile(compressedFileName);

        // 上传切割后的文件到 SFTP 服务器上
        String remotePath = "/file/" + batchDate;
        for (File file : splitFiles) {
            SftpUtils.uploadFile(file, remotePath);
        }

        String finishFileName = "/data/cwy/webank/" + batchDate + "/data." + batchDate + ".finish";
        File finishFile = FileUtil.touch(finishFileName);
        SftpUtils.uploadFile(finishFile, remotePath);

        // 构造邮件正文
        // 构造数据包名
        String dataPackageName = "data." + batchDate + ".pkg.tar.gz";
        // 构造数据日期
        String dataDate = batchDate;
        // 构造数据包内包含的数据文件名
        // 这里假设你的 encryptedFiles 列表中的每个文件都应该被包含在邮件正文中
        StringBuilder dataFileNames = new StringBuilder();
        for (File encryptedFile : encryptedFiles) {
            dataFileNames.append(encryptedFile.getName()).append("\n");
        }
        // 构造邮件正文
        String emailBody = String.format(EMAIL_BODY_TEMPLATE, dataPackageName, dataDate, dataPackageName, dataFileNames);
        // 发送邮件
        EmailUtils.sendEmail(EMAIL_SUBJECT, emailBody);

        DingDingUtils.sendDing(StrUtil.format("微众银行 {} 批次数据提供完成流程结束", batchDate));
    }

    static class TableProcessor implements Callable<File> {

        private String tableName;
        private String batchDate;

        TableProcessor(String tableName, String batchDate) {
            this.tableName = tableName;
            this.batchDate = batchDate;
        }


        /**
         * @Description //TODO 将这些经过数据转换加密后文件收集到一个列表中
         * @Author cwy
         * @Date 2023/6/15 0015
         * @Param
         * @return java.io.File
         **/
        @Override
        public File call() {

            /*File encryptedTableFile = null;
            try {
                if (DatabaseUtils.ifTableExist(tableName)) {
                    String tableStatus = DatabaseUtils.getTableStatus(tableName);
                    File tableFile = null;

                    if ("mysql".equals(tableStatus)) {
                        logger.info("开始转换表 {} 到 textfile 文件", tableName);
                        tableFile = ExportUtils.exportTableToTextFile(tableName, batchDate);

                        DatabaseUtils.updateTableStatus(tableName, "textfile");
                        tableStatus = DatabaseUtils.getTableStatus(tableName);  // 获取更新后的状态
                        logger.info("表 {} 已转换到 textfile 文件", tableName);
                    } else {
                        // 如果表已经转换到 textfile 文件，直接读取该文件
                        String textfilePath = "/data/cwy/webank/" + batchDate + "/" + tableName + ".full.textfile";
                        tableFile = new File(textfilePath);
                        if (!tableFile.exists()) {
                            logger.warn("表 {} 的 textfile 文件 {} 不存在", tableName, textfilePath);
                        }
                    }

                    if ("textfile".equals(tableStatus)) {
                        logger.info("开始加密表 {}", tableName);
                        String encryptedFilePath = tableFile.getParent() + File.separator + tableFile.getName();
                        encryptedTableFile = EncryptUtils.encryptFile(tableFile.getAbsolutePath(), encryptedFilePath);

                        DatabaseUtils.updateTableStatus(tableName, "encrypted");
                        tableStatus = DatabaseUtils.getTableStatus(tableName);  // 获取更新后的状态
                        logger.info("表 {} 已加密", tableName);
                    } else {
                        // 如果表已经加密，直接读取加密后的文件1
                        String encryptedFilePath = "/data/cwy/webank/" + batchDate + "/encrypted_" + tableName + ".full.textfile";
                        encryptedTableFile = new File(encryptedFilePath);
                        logger.info(encryptedTableFile.getAbsolutePath());
                    }

                } else {
                    logger.warn("表 {} 并不真实存在于(129) Mysql 的dw数据库", tableName);
                }
            } catch (Throwable t) {
                logger.error("处理表 {} 时出现错误", tableName, t.getMessage());
            }

            if (encryptedTableFile == null) {
                logger.warn("加密表 {} 的文件为null，跳过此文件", tableName);
            }
            return encryptedTableFile;
        }*/

            File encryptedTableFile = null;
            try {
                if (DatabaseUtils.ifTableExist(tableName)) {
                    logger.info("开始转换表 {} 到 textfile 文件", tableName);
                    File tableFile = ExportUtils.exportTableToTextFile(tableName, batchDate);
                    logger.info("表 {} 已经转换完成到 textfile 文件", tableName);


                    String encryptedFilePath = tableFile.getParent() + File.separator + "encrypted_" + tableFile.getName();

                    logger.info("开始加密表 {}", tableName);
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

            if (encryptedTableFile == null) {
                logger.warn("加密表 {} 的文件为null，跳过此文件", tableName);
            }
            return encryptedTableFile;
        }
    }

}

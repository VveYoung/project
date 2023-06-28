package com.sixlens.project.webank.config;

import cn.hutool.core.date.DateUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @ClassName: WebankConfig
 * @Description: //TODO 
 * @Author: cwy
 * @Date: 2023/6/12 0012 
 * @Version: 2.0
 */
public class WebankConfig {

    public static final String LINUX_DATA_PATH = "/data/cwy/webank/";
//    public static final String LINUX_DATA_PATH = "D:\\data";


    public static final String SFTP_IP = "47.107.254.92";
    public static final int SFTP_PORT = 22333;
    public static final String SFTP_USERNAME = "psbcdev";
    public static final String SFTP_PASSWORD = "7TSVUUHVJPDQ42WF";
    public static final String SFTP_REMOTE_PATH = "/webank/data/";

    public static final String EMAIL_SMTP_HOST = "smtp.qq.com";
    public static final int EMAIL_SMTP_PORT = 25; // 若smtp未加密填25，若采用starttls加密填587，若采用ssl加密填465

    public static final String EMAIL_FROM = "www.563244723@qq.com"; // 邮件发送方
    public static final String EMAIL_USERNAME = "563244723";
    public static final String EMAIL_PASSWORD = "geelhtmtrnukbdjb"; // 为邮件发送方开通smtp服务的授权码

    public static final String EMAIL_TO = "vveyoung@163.com"; // 邮件接受方
}

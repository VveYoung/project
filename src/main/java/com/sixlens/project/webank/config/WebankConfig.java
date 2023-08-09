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

    public static final String SFTP_IP = "171.34.174.207";
    public static final int SFTP_PORT = 9022;
    public static final String SFTP_USERNAME = "liulengjing";
    public static final String SFTP_PASSWORD = "Addmin123";

    public static final String EMAIL_SMTP_HOST = "smtp.exmail.qq.com";
    public static final String EMAIL_SMTP_PROTOCOL = "smtp";
    public static final int EMAIL_SMTP_PORT = 465; // 若smtp未加密填25，若采用starttls加密填587，若采用ssl加密填465

    public static final String EMAIL_FROM = "chenweiyang@linkinip.com"; // 邮件发送方
    public static final String EMAIL_USERNAME = "chenweiyang@linkinip.com";
    public static final String EMAIL_PASSWORD = "82JfRzAV9K4386Xr"; // 为邮件发送方开通smtp服务的授权码

    public static final String[] EMAIL_TO = {"lucylu@webank.com", "liangshuangshuang@linkinip.com", "zhangshuna@linkinip.com"}; // 邮件接受方

}

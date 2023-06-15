package com.sixlens.project.webank.util;

import com.sixlens.project.webank.config.WebankConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @ClassName: EmailUtil
 * @Description: //TODO 用于发送导出结果的邮件通知
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */

public class EmailUtils {

    public static void sendEmail(String subject, String body) {

        Properties properties = new Properties();
        properties.put("mail.smtp.host", WebankConfig.EMAIL_SMTP_HOST);
        properties.put("mail.smtp.port", WebankConfig.EMAIL_SMTP_PORT);  // 根据您的邮件服务器设置
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WebankConfig.EMAIL_USERNAME, WebankConfig.EMAIL_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(WebankConfig.EMAIL_FROM));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(WebankConfig.EMAIL_TO));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public static void sendNotificationEmail(String batch) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", WebankConfig.EMAIL_SMTP_HOST);
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WebankConfig.EMAIL_USERNAME, WebankConfig.EMAIL_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(WebankConfig.EMAIL_FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(WebankConfig.EMAIL_TO));
        message.setSubject("提供数据批次: " + batch);
        message.setText("数据批次 " + batch + " 已经上传完成。");

        Transport.send(message);
    }
}
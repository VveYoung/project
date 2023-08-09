package com.sixlens.project.webank.util;

import com.sixlens.project.webank.config.WebankConfig;
import com.sun.mail.util.MailSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * @ClassName: EmailUtil
 * @Description: //TODO 邮件工具类，用于发送导出结果的邮件通知
 * @Author: cwy
 * @Date: 2023/6/7 0007
 * @Version: 1.0
 */

public class EmailUtils {

    private static Logger logger = LoggerFactory.getLogger(EmailUtils.class);

    /**
     * @Description //TODO 发送邮件
     * @Author cwy
     * @Date 2023/6/16 0016
     * @Param subject 邮件主题
     * @Param body 邮件正文
     * @return void
     **/
    public static void sendEmail(String subject, String body) {

        Properties properties = new Properties();
        properties.put("mail.transport.protocol", WebankConfig.EMAIL_SMTP_PROTOCOL); // 协议
        properties.put("mail.smtp.host", WebankConfig.EMAIL_SMTP_HOST); // 服务器
        properties.put("mail.smtp.port", WebankConfig.EMAIL_SMTP_PORT);  // 端口
        properties.put("mail.smtp.auth", "true"); // 使用stmp身份验证

        // 创建邮件会话的实例
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WebankConfig.EMAIL_USERNAME, WebankConfig.EMAIL_PASSWORD);
            }
        });

        // session.setDebug(true); // 设置调式模式

        try {

            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.ssl.socketFactory", sf);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(WebankConfig.EMAIL_FROM));

            // 给多人推送邮件
            for (String recipient : WebankConfig.EMAIL_TO) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            logger.info("提醒邮件发送成功");

        } catch (MessagingException | GeneralSecurityException e) {
            logger.error("发送邮件失败: {}", e);
            // e.printStackTrace();
        }
    }
}

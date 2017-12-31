package il.co.boj.k300.services.impl;

/**
 * Created by ofer on 31/12/17.
 */

        import il.co.boj.k300.configuration.EmailConfig;
import il.co.boj.k300.services.EmailService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

@Service
@Log4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    EmailConfig emailConfig;

    @Override
    public boolean sendEmailWithAttachments(String[] attachFiles) {

        boolean send = true;

        // get details from properties

        String host = emailConfig.getHost();
        String port = emailConfig.getPort();
        String toAddress = emailConfig.getToAddress();
        String subject = emailConfig.getSubject();
        String message = emailConfig.getMessage();
        final String userName = emailConfig.getUserName();
        final String password = emailConfig.getPassword();

        if (host == null || port == null || toAddress == null || subject == null || message == null) {
            log.debug("One of the parameters needed for sending email is null:  host: [" + host + "] , port: ["
                    + port + "] , sending to: [" + toAddress + "] , " + "mail subject: [" + subject + "] , message: ["
                    + message + "] ,userName:  [" + userName + "] ");
            return false;
        }

        log.debug("Sending pdf with signature via email with the following details: host: [" + host + "] , port: ["
                + port + "] , sending to: [" + toAddress + "] , " + "mail subject: [" + subject + "] , message: ["
                + message + "] ,userName:  [" + userName + "] ");

        try {

            // sets SMTP server properties
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "false");
            properties.put("mail.user", userName);
            properties.put("mail.password", password);

            // creates a new session with an authenticator
            // Authenticator auth = new Authenticator() {
            // public PasswordAuthentication getPasswordAuthentication() {
            // return new PasswordAuthentication(userName, password);
            // }
            // };
            Session session = Session.getInstance(properties);

            // creates a new e-mail message
            Message msg = new MimeMessage(session);

            msg.setFrom(new InternetAddress(userName));
            InternetAddress[] toAddresses = { new InternetAddress(toAddress) };
            msg.setRecipients(Message.RecipientType.TO, toAddresses);
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            // creates message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(message, "text/html");

            // creates multi-part
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // adds attachments
            if (attachFiles != null && attachFiles.length > 0) {
                int counter = 0;
                for (String filePath : attachFiles) {
                    MimeBodyPart attachPart = new MimeBodyPart();

                    try {
                        attachPart.attachFile(filePath);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    counter++;
                    multipart.addBodyPart(attachPart);
                }
                log.debug("Attached [ " + counter + " ] files to email.");
            }

            // sets the multi-part as e-mail's content
            msg.setContent(multipart);

            // sends the e-mail
            Transport.send(msg);
            log.debug("Email has been sent successfully");
            return send;
        } catch (Exception e) {
            send = false;
            log.debug("Failed sending Email. " + e.getMessage());
            return send;
        }

    }

}

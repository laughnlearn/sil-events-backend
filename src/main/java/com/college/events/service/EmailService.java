package com.college.events.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink, String clubNameOrEmail) {
        String subject = "Password Reset - College Events & Resources";
        String htmlBody = """
                <html>
                <body style='font-family: Arial, sans-serif; color: #111827;'>
                    <h2>Password Reset Request</h2>
                    <p>Hello %s,</p>
                    <p>We received a request to reset your password for the College Events & Resources admin portal.</p>
                    <p><a href='%s' style='background:#1d4ed8;color:white;padding:10px 14px;text-decoration:none;border-radius:6px;'>Reset Password</a></p>
                    <p>This link will expire in 15 minutes.</p>
                    <p>If you did not request this reset, you can safely ignore this email.</p>
                </body>
                </html>
                """.formatted(clubNameOrEmail, resetLink);

        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    public void sendAdmin2CredentialsEmail(String toEmail, String clubName, String tempPassword) {
        String subject = "Your ADMIN2 account - College Events & Resources";
        String htmlBody = """
                <html>
                <body style='font-family: Arial, sans-serif; color: #111827;'>
                    <h2>ADMIN2 Account Created</h2>
                    <p>Your club account has been created.</p>
                    <p><strong>Login email:</strong> %s</p>
                    <p><strong>Club username:</strong> %s</p>
                    <p><strong>Temporary password:</strong> %s</p>
                    <p>Please login and change your password immediately.</p>
                </body>
                </html>
                """.formatted(toEmail, clubName, tempPassword);
        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("Unable to send email", ex);
        }
    }
}

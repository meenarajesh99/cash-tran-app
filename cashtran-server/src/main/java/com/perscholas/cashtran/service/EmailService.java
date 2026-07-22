package com.perscholas.cashtran.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final String fromEmail;

  public EmailService(
      JavaMailSender mailSender, @Value("${spring.mail.username}") String fromEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
  }

  public void sendEmail(String to, String username) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setFrom(fromEmail);
    helper.setTo(to);
    helper.setSubject("Welcome to CashTran");

    String htmlContent =
        """
                <html>
                    <body>
                        <h2>Welcome to CashTran, %s!</h2>
                        <p>Your account has been created successfully.</p>
                        <p>You can now login and start transferring money securely.</p>
                        <br>
                        <p>
                        Thanks,<br>
                        CashTran Team
                        </p>
                    </body>
                </html>
                """
            .formatted(username);

    helper.setText(htmlContent, true);
    System.out.println("Sending email through Gmail SMTP");
    mailSender.send(message);
    System.out.println("SMTP accepted email");
  }
}

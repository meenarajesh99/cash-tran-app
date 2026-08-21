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

  public void sendPasswordResetEmail(String to, String username, String resetLink)
      throws MessagingException {

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setFrom(fromEmail);
    helper.setTo(to);
    helper.setSubject("CashTran Password Reset");

    String htmlContent =
        """
                <html>
                    <body>
                        <h2>Password Reset Request</h2>

                        <p>Hello %s,</p>

                        <p>
                            We received a request to reset your CashTran password.
                        </p>

                        <p>
                            <a href="%s">
                                Reset Your Password
                            </a>
                        </p>

                        <p>
                            This link will expire in 30 minutes.
                        </p>

                        <p>
                            If you did not request a password reset,
                            you can safely ignore this email.
                        </p>

                        <br>

                        <p>
                            Thanks,<br>
                            CashTran Team
                        </p>
                    </body>
                </html>
                """
            .formatted(username, resetLink);

    helper.setText(htmlContent, true);

    mailSender.send(message);
  }
}

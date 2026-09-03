package com.perscholas.cashtran.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private final JavaMailSender mailSender;
  private final String fromEmail;

  public EmailService(
      JavaMailSender mailSender, @Value("${spring.mail.username}") String fromEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
  }

  @Async
  public void sendEmail(String to, String username) {
    try {
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

      log.info("Sending welcome email to {}", to);
      mailSender.send(message);
      log.info("Welcome email sent to {}", to);

    } catch (Exception e) {
      log.error("Unable to send welcome email to {}", to, e);
    }
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

  public void sendTransferNotification(
      String to,
      String username,
      String otherUsername,
      BigDecimal amount,
      String transferType,
      String status)
      throws MessagingException {

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);

    helper.setFrom(fromEmail);
    helper.setTo(to);

    String subject;

    if ("Send".equalsIgnoreCase(transferType)) {
      subject = "CashTran - You Received a Transfer";
    } else if ("Approved".equalsIgnoreCase(status)) {
      subject = "CashTran - Money Request Approved";
    } else if ("Rejected".equalsIgnoreCase(status)) {
      subject = "CashTran - Money Request Rejected";
    } else {
      subject = "CashTran - Money Request";
    }

    helper.setSubject(subject);

    String htmlContent =
        """
                <html>
                    <body>
                        <h2>CashTran Transfer Notification</h2>

                        <p>Hello %s,</p>

                        <p>
                            %s
                        </p>

                        <p>
                            <strong>Other User:</strong> %s<br>
                            <strong>Amount:</strong> $%s<br>
                            <strong>Type:</strong> %s<br>
                            <strong>Status:</strong> %s
                        </p>

                        <br>

                        <p>
                            Thanks,<br>
                            CashTran Team
                        </p>
                    </body>
                </html>
                """
            .formatted(
                username,
                getTransferMessage(transferType, status),
                otherUsername,
                amount.setScale(2, RoundingMode.HALF_UP),
                transferType,
                status);

    helper.setText(htmlContent, true);

    mailSender.send(message);
  }

  private String getTransferMessage(String transferType, String status) {

    if ("Send".equalsIgnoreCase(transferType)) {
      return "You have received a money transfer.";
    }

    if ("Pending".equalsIgnoreCase(status)) {
      return "You have received a new money request.";
    }

    if ("Approved".equalsIgnoreCase(status)) {
      return "Your money request has been approved.";
    }

    if ("Rejected".equalsIgnoreCase(status)) {
      return "Your money request has been rejected.";
    }

    return "There has been an update to your CashTran transaction.";
  }
}

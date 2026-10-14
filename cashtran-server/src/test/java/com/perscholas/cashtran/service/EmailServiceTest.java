package com.perscholas.cashtran.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock JavaMailSender mailSender;

  private EmailService emailService;

  private static final String FROM_EMAIL = "cashtran@example.com";

  @BeforeEach
  void setUp() {
    emailService = new EmailService(mailSender, FROM_EMAIL);
  }

  // -------------------------------------------------------------------------
  // Welcome Email
  // -------------------------------------------------------------------------

  @Test
  void sendEmailCreatesCorrectWelcomeEmail() throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    emailService.sendEmail("alice@example.com", "alice");

    assertEmailMetadata(message, FROM_EMAIL, "alice@example.com", "Welcome to CashTran");

    String body = getBody(message);

    assertAll(
        () -> assertTrue(body.contains("Welcome to CashTran, alice!")),
        () -> assertTrue(body.contains("Your account has been created successfully.")),
        () -> assertTrue(body.contains("start transferring money securely")),
        () -> assertTrue(body.contains("CashTran Team")));

    verify(mailSender).send(message);
  }

  @Test
  void sendEmailHandlesMailSendException() {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    whenSendThrowsMailSendException(message);

    assertDoesNotThrow(() -> emailService.sendEmail("alice@example.com", "alice"));

    verify(mailSender).send(message);
  }

  // -------------------------------------------------------------------------
  // Password Reset Email
  // -------------------------------------------------------------------------

  @Test
  void sendPasswordResetEmailCreatesCorrectEmail() throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    String resetLink = "https://cashtran.example.com/reset-password?token=abc123";

    emailService.sendPasswordResetEmail("alice@example.com", "alice", resetLink);

    assertEmailMetadata(message, FROM_EMAIL, "alice@example.com", "CashTran Password Reset");

    String body = getBody(message);

    assertAll(
        () -> assertTrue(body.contains("Password Reset Request")),
        () -> assertTrue(body.contains("Hello alice")),
        () -> assertTrue(body.contains(resetLink)),
        () -> assertTrue(body.contains("30 minutes")),
        () -> assertTrue(body.contains("CashTran Team")));

    verify(mailSender).send(message);
  }

  @Test
  void sendPasswordResetEmailPropagatesMailSendException() {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    MailSendException mailSendException = new MailSendException("SMTP server unavailable");

    doThrow(mailSendException).when(mailSender).send(message);

    MailSendException thrown =
        assertThrows(
            MailSendException.class,
            () ->
                emailService.sendPasswordResetEmail(
                    "alice@example.com",
                    "alice",
                    "https://cashtran.example.com/reset-password?token=abc123"));

    assertSame(mailSendException, thrown);

    verify(mailSender).send(message);
  }

  // -------------------------------------------------------------------------
  // Transfer Notification Emails
  // -------------------------------------------------------------------------

  @ParameterizedTest
  @MethodSource("transferEmailCases")
  void sendTransferNotificationCreatesCorrectEmail(
      String recipient,
      String username,
      String otherUsername,
      BigDecimal amount,
      String transferType,
      String status,
      String expectedSubject,
      String expectedMessage)
      throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    emailService.sendTransferNotification(
        recipient, username, otherUsername, amount, transferType, status);

    assertEmailMetadata(message, FROM_EMAIL, recipient, expectedSubject);

    String body = getBody(message);

    assertAll(
        () -> assertTrue(body.contains("CashTran Transfer Notification")),
        () -> assertTrue(body.contains("Hello " + username)),
        () -> assertTrue(body.contains(expectedMessage)),
        () -> assertTrue(body.contains(otherUsername)),
        () -> assertTrue(body.contains("$" + amount.setScale(2, RoundingMode.HALF_UP))),
        () -> assertTrue(body.contains(transferType)),
        () -> assertTrue(body.contains(status)),
        () -> assertTrue(body.contains("CashTran Team")));

    verify(mailSender).send(message);
  }

  static Stream<Arguments> transferEmailCases() {

    return Stream.of(
        Arguments.of(
            "bob@example.com",
            "bob",
            "alice",
            new BigDecimal("25.00"),
            "Send",
            "Completed",
            "CashTran - You Received a Transfer",
            "You have received a money transfer."),
        Arguments.of(
            "bob@example.com",
            "bob",
            "alice",
            new BigDecimal("50.00"),
            "Request",
            "Pending",
            "CashTran - Money Request",
            "You have received a new money request."),
        Arguments.of(
            "alice@example.com",
            "alice",
            "bob",
            new BigDecimal("20.00"),
            "Request",
            "Approved",
            "CashTran - Money Request Approved",
            "Your money request has been approved."),
        Arguments.of(
            "alice@example.com",
            "alice",
            "bob",
            new BigDecimal("20.00"),
            "Request",
            "Rejected",
            "CashTran - Money Request Rejected",
            "Your money request has been rejected."));
  }

  @Test
  void sendTransferNotificationFormatsAmountToTwoDecimalPlaces() throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    emailService.sendTransferNotification(
        "bob@example.com", "bob", "alice", new BigDecimal("25.5"), "Send", "Completed");

    String body = getBody(message);

    assertTrue(body.contains("$25.50"));

    verify(mailSender).send(message);
  }

  @Test
  void sendTransferNotificationUsesFallbackForUnknownStatus() throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    emailService.sendTransferNotification(
        "bob@example.com", "bob", "alice", new BigDecimal("15.00"), "Request", "Unknown");

    assertEquals("CashTran - Money Request", message.getSubject());

    String body = getBody(message);

    assertTrue(body.contains("There has been an update to your CashTran transaction."));

    assertTrue(body.contains("Unknown"));

    verify(mailSender).send(message);
  }

  @Test
  void sendTransferNotificationHandlesCaseInsensitiveValues() throws Exception {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    emailService.sendTransferNotification(
        "bob@example.com", "bob", "alice", new BigDecimal("15.00"), "send", "completed");

    assertEquals("CashTran - You Received a Transfer", message.getSubject());

    String body = getBody(message);

    assertTrue(body.contains("You have received a money transfer."));

    verify(mailSender).send(message);
  }

  @Test
  void sendTransferNotificationHandlesMailSendException() {

    MimeMessage message = realMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(message);

    whenSendThrowsMailSendException(message);

    assertDoesNotThrow(
        () ->
            emailService.sendTransferNotification(
                "bob@example.com", "bob", "alice", new BigDecimal("25.00"), "Send", "Completed"));

    verify(mailSender).send(message);
  }

  // -------------------------------------------------------------------------
  // Test Helpers
  // -------------------------------------------------------------------------

  private MimeMessage realMimeMessage() {

    Session session = Session.getInstance(new Properties());

    return new MimeMessage(session);
  }

  private void whenSendThrowsMailSendException(MimeMessage message) {

    doThrow(new MailSendException("SMTP server unavailable")).when(mailSender).send(message);
  }

  private String getBody(MimeMessage message) throws Exception {

    StringBuilder body = new StringBuilder();

    extractText(message, body);

    return body.toString();
  }

  private void extractText(Part part, StringBuilder body) throws Exception {

    Object content = part.getContent();

    if (content instanceof String) {
      body.append(content);
      return;
    }

    if (content instanceof Multipart multipart) {

      for (int i = 0; i < multipart.getCount(); i++) {

        extractText(multipart.getBodyPart(i), body);
      }
    }
  }

  private void assertEmailMetadata(
      MimeMessage message, String expectedFrom, String expectedTo, String expectedSubject)
      throws Exception {

    assertEquals(expectedFrom, message.getFrom()[0].toString());

    assertEquals(expectedTo, message.getAllRecipients()[0].toString());

    assertEquals(expectedSubject, message.getSubject());
  }
}

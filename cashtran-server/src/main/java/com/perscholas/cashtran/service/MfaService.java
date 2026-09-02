package com.perscholas.cashtran.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();

  public String generateSecret() {
    return secretGenerator.generate();
  }

  public boolean verifyCode(String secret, String code) {

    if (secret == null || code == null) {
      return false;
    }

    System.out.println("MFA secret exists: " + !secret.isBlank());
    System.out.println("MFA code received: " + code);

    CodeVerifier verifier =
        new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());

    ((DefaultCodeVerifier) verifier).setTimePeriod(30);
    ((DefaultCodeVerifier) verifier).setAllowedTimePeriodDiscrepancy(1);

    boolean valid = verifier.isValidCode(secret, code);

    System.out.println("MFA code valid: " + valid);

    return valid;
  }

  public String generateOtpAuthUrl(String username, String secret) {

    return String.format("otpauth://totp/CashTran:%s?secret=%s&issuer=CashTran", username, secret);
  }
}

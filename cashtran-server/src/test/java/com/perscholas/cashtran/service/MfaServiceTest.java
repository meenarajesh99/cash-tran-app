package com.perscholas.cashtran.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MfaServiceTest {

    @Test
    void generateSecretAndOtpUrl() {
        MfaService mfa = new MfaService();

        String secret = mfa.generateSecret();
        assertNotNull(secret);
        assertFalse(secret.isBlank());

        String url = mfa.generateOtpAuthUrl("alice", secret);
        assertTrue(url.contains("alice"));
        assertTrue(url.contains(secret));
    }

    @Test
    void verifyValidAndInvalidCodes() throws Exception {
        MfaService mfa = new MfaService();
        String secret = mfa.generateSecret();

        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        SystemTimeProvider tp = new SystemTimeProvider();
        String code = null;

        // The generator API expects a time value; different implementations use seconds vs millis.
        // Try a couple of common options and accept whichever verifies successfully.
        long[] candidates = new long[] { tp.getTime(), tp.getTime() / 1000L, tp.getTime() / 30L };
        for (long t : candidates) {
            try {
                code = generator.generate(secret, t);
            } catch (Exception e) {
                // ignore and try next candidate
                continue;
            }
            if (mfa.verifyCode(secret, code)) {
                break;
            }
        }

        assertNotNull(code, "should have generated at least one code candidate");
        assertTrue(mfa.verifyCode(secret, code), "generated code should verify successfully");

        // invalid cases
        assertFalse(mfa.verifyCode(null, code));
        assertFalse(mfa.verifyCode(secret, null));
        assertFalse(mfa.verifyCode(secret, "000000"));
    }
}




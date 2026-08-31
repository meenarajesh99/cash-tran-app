package com.perscholas.cashtran.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenProviderTest {

  private TokenProvider provider() {
    byte[] secretBytes = new byte[64];
    for (int i = 0; i < secretBytes.length; i++) {
      secretBytes[i] = (byte) (i + 1);
    }
    TokenProvider provider =
        new TokenProvider(Base64.getEncoder().encodeToString(secretBytes), 60, 120);
    provider.init();
    return provider;
  }

  @Test
  void createsValidTokenWithSubjectAndAuthorities() {
    TokenProvider provider = provider();

    String token =
        provider.createToken(
            new UsernamePasswordAuthenticationToken(
                "alice", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    assertTrue(provider.validateToken(token));
    assertEquals("alice", provider.getUserNameFromToken(token));

    assertTrue(
        provider
            .getAuthentication(token)
            .getAuthorities()
            .contains(new SimpleGrantedAuthority("ROLE_USER")));
  }

  @Test
  void rejectsMalformedToken() {
    assertFalse(provider().validateToken("not-a-token"));
  }
}

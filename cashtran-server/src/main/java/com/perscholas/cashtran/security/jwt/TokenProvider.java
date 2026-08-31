package com.perscholas.cashtran.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

  private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);
  private static final String AUTHORITIES_KEY = "auth";

  /*
   * Claim used to identify a temporary MFA challenge token.
   */
  private static final String MFA_CHALLENGE_CLAIM = "mfa_challenge";

  /*
   * MFA challenge expires after 5 minutes.
   */
  private static final long MFA_CHALLENGE_VALIDITY = 5 * 60 * 1000L;
  private final String base64Secret;
  private final long tokenValidityInMilliseconds;
  private final long tokenValidityInMillisecondsForRememberMe;
  private SecretKey key;

  public TokenProvider(
      @Value("${jwt.base64-secret}") String base64Secret,
      @Value("${jwt.token-validity-in-seconds}") long tokenValidityInSeconds,
      @Value("${jwt.token-validity-in-seconds-for-remember-me}")
          long tokenValidityInSecondsForRememberMe) {

    this.base64Secret = base64Secret;
    this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    this.tokenValidityInMillisecondsForRememberMe = tokenValidityInSecondsForRememberMe * 1000;
  }

  /*
   * Initialize the JWT signing key.
   */
  @PostConstruct
  public void init() {
    byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  /*
   * ============================================================
   * NORMAL JWT
   * ============================================================
   */

  public String createToken(Authentication authentication, boolean rememberMe) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    long now = System.currentTimeMillis();
    Date validity =
        new Date(
            now
                + (rememberMe
                    ? tokenValidityInMillisecondsForRememberMe
                    : tokenValidityInMilliseconds));

    return io.jsonwebtoken.Jwts.builder()
        .setSubject(authentication.getName())
        .claim(AUTHORITIES_KEY, authorities)
        .setIssuedAt(new Date(now))
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  public String createToken(Authentication authentication) {

    return createToken(authentication, false);
  }

  /*
   * Extract Spring Security Authentication
   * from a normal JWT.
   */
  public Authentication getAuthentication(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    String authoritiesClaim = claims.get(AUTHORITIES_KEY, String.class);
    Collection<? extends GrantedAuthority> authorities;
    if (authoritiesClaim == null || authoritiesClaim.isBlank()) {
      authorities = java.util.List.of();
    } else {
      authorities =
          Arrays.stream(authoritiesClaim.split(","))
              .filter(auth -> !auth.isBlank())
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());
    }

    org.springframework.security.core.userdetails.User principal =
        new org.springframework.security.core.userdetails.User(
            claims.getSubject(), "", authorities);
    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  /*
   * Get username from a normal JWT.
   */
  public String getUserNameFromToken(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return claims.getSubject();
  }

  /*
   * Validate a normal JWT.
   *
   * IMPORTANT:
   * MFA challenge tokens should NOT be treated
   * as normal authentication tokens.
   */
  public boolean validateToken(String authToken) {

    try {
      Claims claims =
          Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken).getPayload();

      /*
       * Reject MFA challenge tokens.
       */
      Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);
      if (Boolean.TRUE.equals(mfaChallenge)) {
        log.warn("MFA challenge token cannot be used as a normal JWT");
        return false;
      }
      return true;

    } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
      log.warn("Invalid JWT signature: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.warn("Expired JWT token: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.warn("Unsupported JWT token: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  /*
   * ============================================================
   * MFA CHALLENGE JWT
   * ============================================================
   */

  /*
   * Creates a temporary token after the
   * username/password are successfully verified.
   *
   * This is NOT a login JWT.
   *
   * It can only be used to complete MFA.
   */
  public String createMfaChallengeToken(String username) {
    long now = System.currentTimeMillis();
    Date validity = new Date(now + MFA_CHALLENGE_VALIDITY);
    return Jwts.builder()
        .setSubject(username)
        .claim(MFA_CHALLENGE_CLAIM, true)
        .setIssuedAt(new Date(now))
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /*
   * Validate the MFA challenge token.
   */
  public boolean validateMfaChallenge(String token) {

    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);
      return Boolean.TRUE.equals(mfaChallenge);
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid or expired MFA challenge: {}", e.getMessage());
      return false;
    }
  }

  /*
   * Get username from MFA challenge token.
   */
  public String getUsernameFromMfaChallenge(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);
    if (!Boolean.TRUE.equals(mfaChallenge)) {
      throw new IllegalArgumentException("Token is not an MFA challenge");
    }
    return claims.getSubject();
  }
}

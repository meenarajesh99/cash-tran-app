package com.perscholas.cashtran.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.SecurityException;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

  private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

  private static final String AUTHORITIES_KEY = "auth";

  /*
   * Claims used to distinguish temporary MFA tokens
   * from normal authentication JWTs.
   */
  private static final String MFA_CHALLENGE_CLAIM = "mfa_challenge";
  private static final String MFA_ENROLLMENT_CLAIM = "mfa_enrollment";

  /*
   * MFA enrollment token is valid for 10 minutes.
   */
  private static final long MFA_ENROLLMENT_VALIDITY = 10 * 60 * 1000L;

  /*
   * MFA login challenge token is valid for 5 minutes.
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
   * ============================================================
   * JWT SIGNING KEY
   * ============================================================
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

  /** Creates the normal authentication JWT. */
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

    return Jwts.builder()
        .setSubject(authentication.getName())
        .claim(AUTHORITIES_KEY, authorities)
        .setIssuedAt(new Date(now))
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /** Creates a normal authentication JWT without remember-me. */
  public String createToken(Authentication authentication) {
    return createToken(authentication, false);
  }

  /** Extracts Spring Security Authentication from a normal JWT. */
  public Authentication getAuthentication(String token) {

    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    String authoritiesClaim = claims.get(AUTHORITIES_KEY, String.class);

    Collection<? extends GrantedAuthority> authorities;

    if (authoritiesClaim == null || authoritiesClaim.isBlank()) {

      authorities = List.of();

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

  /** Gets the username from a normal JWT. */
  public String getUserNameFromToken(String token) {

    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    return claims.getSubject();
  }

  /**
   * Validates a normal authentication JWT.
   *
   * <p>MFA challenge and MFA enrollment tokens are intentionally rejected here.
   */
  public boolean validateToken(String authToken) {

    try {

      Claims claims =
          Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken).getPayload();

      /*
       * MFA login challenge tokens cannot be used
       * as normal authentication JWTs.
       */
      Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);

      if (Boolean.TRUE.equals(mfaChallenge)) {

        log.warn("MFA challenge token cannot be used as a normal JWT");

        return false;
      }

      /*
       * MFA enrollment tokens cannot be used
       * as normal authentication JWTs.
       */
      Boolean mfaEnrollment = claims.get(MFA_ENROLLMENT_CLAIM, Boolean.class);

      if (Boolean.TRUE.equals(mfaEnrollment)) {

        log.warn("MFA enrollment token cannot be used as a normal JWT");

        return false;
      }

      return true;

    } catch (SecurityException | MalformedJwtException e) {

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
   * MFA LOGIN CHALLENGE TOKEN
   * ============================================================
   */

  /**
   * Creates a temporary MFA challenge token after username/password authentication succeeds.
   *
   * <p>This is NOT a login JWT.
   *
   * <p>It can only be used to complete MFA login.
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

  /** Validates an MFA login challenge token. */
  public boolean validateMfaChallenge(String token) {

    try {

      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

      Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);

      return Boolean.TRUE.equals(mfaChallenge);

    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {

      log.warn("Invalid or expired MFA challenge: {}", e.getMessage());

      return false;
    }
  }

  /** Gets the username from an MFA login challenge token. */
  public String getUsernameFromMfaChallenge(String token) {

    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    Boolean mfaChallenge = claims.get(MFA_CHALLENGE_CLAIM, Boolean.class);

    if (!Boolean.TRUE.equals(mfaChallenge)) {

      throw new IllegalArgumentException("Token is not an MFA challenge");
    }

    return claims.getSubject();
  }

  /*
   * ============================================================
   * MFA REGISTRATION ENROLLMENT TOKEN
   * ============================================================
   */

  /**
   * Creates a temporary enrollment token during registration.
   *
   * <p>The user receives this token along with the MFA QR code.
   *
   * <p>It is used to verify the first Google Authenticator code.
   */
  public String createMfaEnrollmentToken(String username) {

    long now = System.currentTimeMillis();

    Date validity = new Date(now + MFA_ENROLLMENT_VALIDITY);

    return Jwts.builder()
        .setSubject(username)
        .claim(MFA_ENROLLMENT_CLAIM, true)
        .setIssuedAt(new Date(now))
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /** Validates an MFA registration enrollment token. */
  public boolean validateMfaEnrollment(String token) {

    try {

      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

      Boolean enrollment = claims.get(MFA_ENROLLMENT_CLAIM, Boolean.class);

      return Boolean.TRUE.equals(enrollment);

    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {

      log.warn("Invalid or expired MFA enrollment token: {}", e.getMessage());

      return false;
    }
  }

  /** Gets the username from an MFA enrollment token. */
  public String getUsernameFromMfaEnrollment(String token) {

    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    Boolean enrollment = claims.get(MFA_ENROLLMENT_CLAIM, Boolean.class);

    if (!Boolean.TRUE.equals(enrollment)) {

      throw new IllegalArgumentException("Token is not an MFA enrollment token");
    }

    return claims.getSubject();
  }
}

package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class JwtUtils {
  @Value("${spring.app.jwtSecret}")
  private String jwtSecret;

  @Value("${spring.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  @Value("${spring.ecom.app.jwtCookieName}")
  private String jwtCookie;

  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  public String getJwtFromCookies(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, jwtCookie);
    if (cookie != null) {
      System.out.println("COOKIE: " + cookie);
      return cookie.getValue();
    } else {
      return null;
    }
  }

  public String getJwtFromHeader(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
    String jwt = generateTokenFromUsername(userPrincipal.getUsername());
    return ResponseCookie.from(jwtCookie, jwt)
        .path("/api")
        .maxAge(24 * 60 * 60)
        .httpOnly(false)
        .build();
  }

  public ResponseCookie getCleanJwtCookie() {
    return ResponseCookie.from(jwtCookie, null).path("/api").build();
  }

  public String generateTokenFromUsername(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date((new Date().getTime() + jwtExpirationMs)))
        .signWith(key())
        .compact();
  }

  public String getUserNameFromJWTToken(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) key())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public boolean validateJwtToken(String authToken) {
    try {
      System.out.println("Validate");
      Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
      return true;
    } catch (MalformedJwtException exception) {
      logger.error("Invalid JWT token: {}", exception.getMessage());
    } catch (ExpiredJwtException exception) {
      logger.error("JWT token is expired: {}", exception.getMessage());
    } catch (UnsupportedJwtException exception) {
      logger.error("JWT token is unsupported: {}", exception.getMessage());
    } catch (IllegalArgumentException exception) {
      logger.error("JWT claim string is empty: {}", exception.getMessage());
    }
    return false;
  }
}

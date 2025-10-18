package com.ecommerce.project.security.jwt;

import java.util.Date;
import org.slf4j.Logger;
import java.security.Key;
import io.jsonwebtoken.Jwts;
import javax.crypto.SecretKey;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class JwtUtils {
  @Value("${spring.app.jwtSecret}")
  private String jwtSecret;

  @Value("${spring.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  public String getJwtFromHeader(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    logger.debug("Authorization Header : {}", bearerToken);
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) return bearerToken.substring(7);
    return null;
  }

  public String generateTokenFromUsername(UserDetails userDetails) {
    String username = userDetails.getUsername();
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

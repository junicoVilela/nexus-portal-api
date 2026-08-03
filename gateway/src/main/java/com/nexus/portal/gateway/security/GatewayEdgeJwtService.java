package com.nexus.portal.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validação JWT no edge para pedidos {@code /api/doc-flow/**}. Usa as mesmas propriedades que o Doc Flow
 * ({@code docflow.security.jwt-secret}).
 */
@Component("gatewayEdgeJwtService")
public class GatewayEdgeJwtService {

  private final SecretKey secretKey;

  public GatewayEdgeJwtService(
      @Value("${docflow.security.jwt-secret:nexus-portal-super-secret-key-please-change-in-production-min-256bits}") String secret) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims validar(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isValido(String token) {
    try {
      validar(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}

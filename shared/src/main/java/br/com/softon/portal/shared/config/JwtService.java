package br.com.softon.portal.shared.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  public static final String CLAIM_TOKEN_TYPE = "typ";
  public static final String TYPE_PACOTE_DOWNLOAD = "PACOTE_ZIP";

  private final SecretKey secretKey;
  private final long expirationMs;
  private final long downloadExpirationMs;

  public JwtService(
      @Value("${docflow.security.jwt-secret:softon-portal-super-secret-key-please-change-in-production-min-256bits}") String secret,
      @Value("${docflow.security.jwt-expiration-ms:86400000}") long expirationMs,
      @Value("${docflow.security.jwt-download-expiration-ms:900000}") long downloadExpirationMs) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
    this.downloadExpirationMs = downloadExpirationMs;
  }

  public String gerarToken(String username, List<String> permissoes) {
    return gerarToken(username, permissoes, UUID.randomUUID().toString());
  }

  public String gerarToken(String username, List<String> permissoes, String jti) {
    return Jwts.builder()
        .subject(username)
        .id(jti)
        .claim("permissoes", permissoes == null ? List.of() : permissoes)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(secretKey)
        .compact();
  }

  public long expirationMs() {
    return expirationMs;
  }

  public String gerarTokenDownloadPacote(UUID publicacaoId) {
    Date exp = new Date(System.currentTimeMillis() + Math.max(60_000L, downloadExpirationMs));
    return Jwts.builder()
        .subject("download-publicacao")
        .claim(CLAIM_TOKEN_TYPE, TYPE_PACOTE_DOWNLOAD)
        .claim("publicacaoId", publicacaoId.toString())
        .issuedAt(new Date())
        .expiration(exp)
        .signWith(secretKey)
        .compact();
  }

  /** Valida JWT de download temporário do ZIP; retorna o id da publicação. */
  public UUID parseTokenDownloadPacoteValido(String token) {
    Claims claims = validar(token);
    if (!TYPE_PACOTE_DOWNLOAD.equals(claims.get(CLAIM_TOKEN_TYPE))) {
      throw new IllegalArgumentException("Token inválido para download.");
    }
    String pid = claims.get("publicacaoId", String.class);
    if (pid == null || pid.isBlank()) {
      throw new IllegalArgumentException("Token sem publicação.");
    }
    return UUID.fromString(pid);
  }

  public long downloadTtlMillis() {
    return Math.max(60_000L, downloadExpirationMs);
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

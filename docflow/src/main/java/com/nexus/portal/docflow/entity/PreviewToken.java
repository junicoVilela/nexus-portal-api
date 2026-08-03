package com.nexus.portal.docflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_preview_token")
public class PreviewToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "cliente_id", nullable = false)
  private UUID clienteId;

  @Column(nullable = false, unique = true, length = 120)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  public PreviewToken(UUID clienteId, String token, OffsetDateTime expiresAt, String createdBy) {
    this.clienteId = clienteId;
    this.token = token;
    this.expiresAt = expiresAt;
    this.createdAt = OffsetDateTime.now();
    this.createdBy = createdBy;
  }

  public void revogar() {
    this.ativo = false;
  }

  public boolean estaValido() {
    return ativo && OffsetDateTime.now().isBefore(expiresAt);
  }
}

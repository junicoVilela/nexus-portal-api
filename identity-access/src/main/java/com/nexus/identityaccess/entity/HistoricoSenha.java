package com.nexus.identityaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
@Table(name = "tb_historico_senha")
public class HistoricoSenha {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;

  @Column(name = "senha_hash", nullable = false, length = 255)
  private String senhaHash;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public HistoricoSenha(UUID usuarioId, String senhaHash) {
    this.usuarioId = usuarioId;
    this.senhaHash = senhaHash;
  }

  @PrePersist
  void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }
}

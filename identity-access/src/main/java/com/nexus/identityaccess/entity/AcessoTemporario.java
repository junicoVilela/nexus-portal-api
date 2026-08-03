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
@Table(name = "tb_acesso_temporario")
public class AcessoTemporario {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "usuario_id", nullable = false)
  private UUID usuarioId;

  @Column(name = "grupo_id")
  private UUID grupoId;

  @Column(name = "permissao_id")
  private UUID permissaoId;

  @Column(name = "escopo_id")
  private UUID escopoId;

  @Column(name = "inicio_em", nullable = false)
  private OffsetDateTime inicioEm;

  @Column(name = "fim_em", nullable = false)
  private OffsetDateTime fimEm;

  @Column(length = 500)
  private String justificativa;

  @Column(name = "revogado_em")
  private OffsetDateTime revogadoEm;

  @Column(name = "motivo_revogacao", length = 200)
  private String motivoRevogacao;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  public AcessoTemporario(UUID usuarioId, UUID grupoId, UUID permissaoId, UUID escopoId,
      OffsetDateTime inicioEm, OffsetDateTime fimEm, String justificativa, String createdBy) {
    this.usuarioId = usuarioId;
    this.grupoId = grupoId;
    this.permissaoId = permissaoId;
    this.escopoId = escopoId;
    this.inicioEm = inicioEm;
    this.fimEm = fimEm;
    this.justificativa = justificativa;
    this.createdBy = createdBy;
  }

  public void revogar(String motivo) {
    this.revogadoEm = OffsetDateTime.now();
    this.motivoRevogacao = motivo;
  }

  /** Status derivado — não persiste. */
  public String status() {
    OffsetDateTime agora = OffsetDateTime.now();
    if (revogadoEm != null) return "REVOGADO";
    if (agora.isAfter(fimEm)) return "EXPIRADO";
    if (agora.isBefore(inicioEm)) return "AGENDADO";
    return "ATIVO";
  }

  @PrePersist
  void prePersist() {
    if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
  }
}

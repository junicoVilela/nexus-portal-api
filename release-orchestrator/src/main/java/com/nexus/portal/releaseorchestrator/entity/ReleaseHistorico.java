package com.nexus.portal.releaseorchestrator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tb_release_historico")
public class ReleaseHistorico {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "release_id", nullable = false)
  private UUID releaseId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AcaoHistorico acao;

  @Column(length = 500)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(name = "status_anterior", length = 30)
  private ReleaseStatus statusAnterior;

  @Enumerated(EnumType.STRING)
  @Column(name = "status_novo", length = 30)
  private ReleaseStatus statusNovo;

  @Column(length = 120)
  private String usuario;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public ReleaseHistorico(UUID releaseId, AcaoHistorico acao, String descricao,
      ReleaseStatus statusAnterior, ReleaseStatus statusNovo, String usuario) {
    this.releaseId = releaseId;
    this.acao = acao;
    this.descricao = descricao;
    this.statusAnterior = statusAnterior;
    this.statusNovo = statusNovo;
    this.usuario = usuario;
  }

  @PrePersist
  void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }
}

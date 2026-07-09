package br.com.softon.rbac.entity;

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
@Table(name = "tb_auditoria_evento")
public class AuditoriaEvento {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 80)
  private String entidade;

  @Column(name = "entidade_id")
  private UUID entidadeId;

  @Column(nullable = false, length = 80)
  private String acao;

  private String descricao;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  public AuditoriaEvento(String entidade, UUID entidadeId, String acao, String descricao, String createdBy) {
    this.entidade = entidade;
    this.entidadeId = entidadeId;
    this.acao = acao;
    this.descricao = descricao;
    this.createdBy = createdBy;
  }

  @PrePersist
  void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }
}

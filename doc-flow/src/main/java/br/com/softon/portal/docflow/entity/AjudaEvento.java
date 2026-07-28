package br.com.softon.portal.docflow.entity;

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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ajuda_evento")
public class AjudaEvento {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoAjudaEvento tipo;

  @Column(name = "conteudo_codigo", length = 80)
  private String conteudoCodigo;

  @Column(length = 240)
  private String termo;

  @Column(length = 240)
  private String rota;

  @Column(name = "sessao_id", length = 80)
  private String sessaoId;

  @Column(name = "resultado_quantidade")
  private Integer resultadoQuantidade;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by", length = 120, updatable = false)
  private String createdBy;

  public AjudaEvento(TipoAjudaEvento tipo, String conteudoCodigo, String termo, String rota,
      String sessaoId, Integer resultadoQuantidade, String createdBy) {
    this.tipo = tipo;
    this.conteudoCodigo = conteudoCodigo;
    this.termo = termo;
    this.rota = rota;
    this.sessaoId = sessaoId;
    this.resultadoQuantidade = resultadoQuantidade;
    this.createdBy = createdBy;
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}

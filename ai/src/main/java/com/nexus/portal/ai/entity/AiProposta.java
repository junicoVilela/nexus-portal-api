package com.nexus.portal.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_proposta")
public class AiProposta {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sessao_id", nullable = false)
  private AiSessao sessao;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private AiJob job;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiPropostaTipo tipo;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, length = 200)
  private String slug;

  @Column(name = "codigo_tela", nullable = false, length = 120)
  private String codigoTela;

  @Column(columnDefinition = "text")
  private String resumo;

  @Column(name = "conteudo_html", nullable = false, columnDefinition = "text")
  private String conteudoHtml;

  @Column(name = "template_id")
  private UUID templateId;

  @Column(name = "template_versao")
  private Integer templateVersao;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "qualidade_json", columnDefinition = "jsonb")
  private String qualidadeJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "page_spec_json", columnDefinition = "jsonb")
  private String pageSpecJson;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiPropostaStatus status = AiPropostaStatus.PENDENTE;

  @Column(name = "pagina_id")
  private UUID paginaId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  public AiProposta(
      AiSessao sessao,
      AiJob job,
      AiPropostaTipo tipo,
      String titulo,
      String slug,
      String codigoTela,
      String resumo,
      String conteudoHtml,
      UUID templateId,
      Integer templateVersao,
      String qualidadeJson,
      String pageSpecJson) {
    this.sessao = sessao;
    this.job = job;
    this.tipo = tipo;
    this.titulo = titulo;
    this.slug = slug;
    this.codigoTela = codigoTela;
    this.resumo = resumo;
    this.conteudoHtml = conteudoHtml;
    this.templateId = templateId;
    this.templateVersao = templateVersao;
    this.qualidadeJson = qualidadeJson;
    this.pageSpecJson = pageSpecJson;
  }

  public void aceitar(UUID paginaId) {
    this.status = AiPropostaStatus.ACEITA;
    this.paginaId = paginaId;
    this.updatedAt = OffsetDateTime.now();
  }

  public void descartar() {
    this.status = AiPropostaStatus.DESCARTADA;
    this.updatedAt = OffsetDateTime.now();
  }
}

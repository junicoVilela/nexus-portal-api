package br.com.softon.portal.docflow.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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
import jakarta.persistence.Version;
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
@Table(name = "tb_pagina")
public class Pagina extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, unique = true, length = 200)
  private String slug;

  @Column(name = "codigo_tela", nullable = false, unique = true, length = 120)
  private String codigoTela;

  private String resumo;

  @Column(name = "conteudo_html")
  private String conteudoHtml;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private StatusPagina status = StatusPagina.RASCUNHO;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_id", nullable = false)
  private Modulo modulo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Pagina parent;

  @Column(name = "published_at")
  private OffsetDateTime publishedAt;

  @Column(name = "template_origem_id")
  private UUID templateOrigemId;

  @Column(name = "template_origem_versao")
  private Integer templateOrigemVersao;

  public Pagina(String titulo, String slug, String codigoTela, String resumo, String conteudoHtml,
      int ordem, boolean ativo, Modulo modulo, Pagina parent) {
    this.titulo = titulo;
    this.slug = slug;
    this.codigoTela = codigoTela;
    this.resumo = resumo;
    this.conteudoHtml = conteudoHtml;
    this.ordem = ordem;
    this.ativo = ativo;
    this.modulo = modulo;
    this.parent = parent;
  }

  public void atualizar(String titulo, String slug, String codigoTela, String resumo, String conteudoHtml,
      int ordem, boolean ativo, Modulo modulo, Pagina parent) {
    this.titulo = titulo;
    this.slug = slug;
    this.codigoTela = codigoTela;
    this.resumo = resumo;
    this.conteudoHtml = conteudoHtml;
    this.ordem = ordem;
    this.ativo = ativo;
    this.modulo = modulo;
    this.parent = parent;
  }

  public void definirOrigemTemplate(UUID templateId, Integer versao) {
    if (templateId != null && versao != null) {
      this.templateOrigemId = templateId;
      this.templateOrigemVersao = versao;
    }
  }

  public void publicar() {
    this.status = StatusPagina.PUBLICADO;
    this.publishedAt = OffsetDateTime.now();
  }

  public void enviarRevisao() {
    this.status = StatusPagina.EM_REVISAO;
  }

  public void aprovar() {
    this.status = StatusPagina.APROVADO;
  }

  public void salvarRascunho() {
    this.status = StatusPagina.RASCUNHO;
  }

  public void arquivar() {
    this.status = StatusPagina.ARQUIVADO;
  }
}

package com.nexus.portal.docflow.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoPagina tipo = TipoPagina.ARTIGO;

  @Column(name = "template_origem_id")
  private UUID templateOrigemId;

  @Column(name = "template_origem_versao")
  private Integer templateOrigemVersao;

  @Column(name = "revisor_username", length = 120)
  private String revisorUsername;

  @Column(name = "prazo_revisao")
  private OffsetDateTime prazoRevisao;

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

  /** Nulo mantém o tipo atual (clientes antigos da API não enviam o campo). */
  public void definirTipo(TipoPagina tipo) {
    if (tipo != null) {
      this.tipo = tipo;
    }
  }

  public boolean menu() {
    return tipo == TipoPagina.MENU;
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

  /**
   * Só mexe na ordem. Reescrever a página inteira para reordenar sujava todos
   * os campos e incrementava a {@code version}, derrubando editores abertos.
   */
  public void definirOrdem(int ordem) {
    if (this.ordem != ordem) {
      this.ordem = ordem;
    }
  }

  /** Ajuste pontual do HTML, sem tocar nos demais campos (usado na duplicação). */
  public void atualizarConteudo(String conteudoHtml) {
    this.conteudoHtml = conteudoHtml;
  }

  public void definirOrigemTemplate(UUID templateId, Integer versao) {
    if (templateId != null && versao != null) {
      this.templateOrigemId = templateId;
      this.templateOrigemVersao = versao;
    }
  }

  public void atribuirRevisor(String revisorUsername, OffsetDateTime prazoRevisao) {
    this.revisorUsername = revisorUsername;
    this.prazoRevisao = prazoRevisao;
  }

  /** Publicada ou devolvida, a atribuição de revisão deixa de valer. */
  public void limparRevisor() {
    this.revisorUsername = null;
    this.prazoRevisao = null;
  }

  public boolean revisaoAtrasada() {
    return prazoRevisao != null
        && status == StatusPagina.EM_REVISAO
        && OffsetDateTime.now().isAfter(prazoRevisao);
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

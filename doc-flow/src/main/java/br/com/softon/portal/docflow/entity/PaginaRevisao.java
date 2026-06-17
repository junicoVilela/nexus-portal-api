package br.com.softon.portal.docflow.entity;

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
@Table(name = "tb_pagina_revisao")
public class PaginaRevisao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pagina_id", nullable = false)
  private Pagina pagina;

  @Column(nullable = false)
  private int numero;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, length = 200)
  private String slug;

  @Column(name = "codigo_tela", nullable = false, length = 120)
  private String codigoTela;

  private String resumo;

  @Column(name = "conteudo_html")
  private String conteudoHtml;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private StatusPagina status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_id", nullable = false)
  private Modulo modulo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Pagina parent;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by", length = 120)
  private String createdBy;

  public PaginaRevisao(Pagina pagina, int numero, String createdBy) {
    this.pagina = pagina;
    this.numero = numero;
    this.titulo = pagina.getTitulo();
    this.slug = pagina.getSlug();
    this.codigoTela = pagina.getCodigoTela();
    this.resumo = pagina.getResumo();
    this.conteudoHtml = pagina.getConteudoHtml();
    this.status = pagina.getStatus();
    this.modulo = pagina.getModulo();
    this.parent = pagina.getParent();
    this.createdBy = createdBy;
  }

  @PrePersist
  void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }
}

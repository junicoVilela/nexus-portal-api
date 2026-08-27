package com.nexus.portal.docflow.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trecho de HTML reutilizável. As páginas referenciam por
 * {@code {{snippet:CODIGO}}} e o conteúdo é resolvido na geração do pacote e no
 * preview — atualizar o snippet reflete em todas as páginas que o citam.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_pagina_snippet")
public class PaginaSnippet extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 60)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(length = 300)
  private String descricao;

  @Column(name = "conteudo_html", nullable = false)
  private String conteudoHtml;

  @Column(nullable = false)
  private boolean ativo = true;

  public PaginaSnippet(String codigo, String titulo, String descricao, String conteudoHtml,
      boolean ativo) {
    this.codigo = codigo;
    this.titulo = titulo;
    this.descricao = descricao;
    this.conteudoHtml = conteudoHtml;
    this.ativo = ativo;
  }

  public void atualizar(String titulo, String descricao, String conteudoHtml, boolean ativo) {
    this.titulo = titulo;
    this.descricao = descricao;
    this.conteudoHtml = conteudoHtml;
    this.ativo = ativo;
  }
}

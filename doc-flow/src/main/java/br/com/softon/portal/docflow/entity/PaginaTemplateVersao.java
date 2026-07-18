package br.com.softon.portal.docflow.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_pagina_template_versao",
    uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "numero"}))
public class PaginaTemplateVersao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id", nullable = false)
  private PaginaTemplate template;

  @Column(nullable = false)
  private int numero;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(length = 300)
  private String descricao;

  @Column(name = "conteudo_html", nullable = false)
  private String conteudoHtml;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "projeto_id")
  private UUID projetoId;

  @Column(name = "cliente_id")
  private UUID clienteId;

  public PaginaTemplateVersao(PaginaTemplate template) {
    this.template = template;
    this.numero = template.getVersaoAtual();
    this.nome = template.getNome();
    this.descricao = template.getDescricao();
    this.conteudoHtml = template.getConteudoHtml();
    this.ativo = template.isAtivo();
    this.projetoId = template.getProjeto() == null ? null : template.getProjeto().getId();
    this.clienteId = template.getCliente() == null ? null : template.getCliente().getId();
  }
}

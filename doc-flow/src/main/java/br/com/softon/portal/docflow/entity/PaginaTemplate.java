package br.com.softon.portal.docflow.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_pagina_template")
public class PaginaTemplate extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 60)
  private String codigo;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(length = 300)
  private String descricao;

  @Column(name = "conteudo_html", nullable = false)
  private String conteudoHtml;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(nullable = false)
  private boolean personalizado;

  @Column(name = "versao_atual", nullable = false)
  private int versaoAtual = 1;

  @ManyToOne
  @JoinColumn(name = "projeto_id")
  private Projeto projeto;

  @ManyToOne
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  public PaginaTemplate(String codigo, String nome, String descricao, String conteudoHtml,
      int ordem, boolean ativo) {
    this.codigo = codigo;
    this.nome = nome;
    this.descricao = descricao;
    this.conteudoHtml = conteudoHtml;
    this.ordem = ordem;
    this.ativo = ativo;
  }

  public PaginaTemplate(String codigo, String nome, String descricao, String conteudoHtml,
      int ordem, Projeto projeto, Cliente cliente) {
    this(codigo, nome, descricao, conteudoHtml, ordem, true);
    this.personalizado = true;
    this.projeto = projeto;
    this.cliente = cliente;
  }

  public void atualizar(String nome, String descricao, String conteudoHtml, Projeto projeto, Cliente cliente) {
    this.nome = nome;
    this.descricao = descricao;
    this.conteudoHtml = conteudoHtml;
    this.projeto = projeto;
    this.cliente = cliente;
    this.versaoAtual++;
  }

  public void definirAtivo(boolean ativo) {
    if (this.ativo != ativo) {
      this.ativo = ativo;
      this.versaoAtual++;
    }
  }

  public void restaurar(String nome, String descricao, String conteudoHtml, boolean ativo,
      Projeto projeto, Cliente cliente) {
    this.nome = nome;
    this.descricao = descricao;
    this.conteudoHtml = conteudoHtml;
    this.ativo = ativo;
    this.projeto = projeto;
    this.cliente = cliente;
    this.versaoAtual++;
  }
}

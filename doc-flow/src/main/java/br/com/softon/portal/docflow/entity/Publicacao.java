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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_publicacao")
public class Publicacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @Column(nullable = false, length = 50)
  private String versao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private StatusPublicacao status = StatusPublicacao.GERANDO;

  @Column(name = "quantidade_paginas", nullable = false)
  private int quantidadePaginas;

  @Column(name = "quantidade_modulos", nullable = false)
  private int quantidadeModulos;

  @Column(name = "arquivo_zip_nome")
  private String arquivoZipNome;

  @Column(name = "arquivo_zip_caminho", length = 500)
  private String arquivoZipCaminho;

  @Column(name = "hash_pacote", length = 120)
  private String hashPacote;

  private String observacao;

  @Column(name = "relatorio_validacao", length = 4000)
  private String relatorioValidacao;

  @Column(name = "arvore_paginas")
  private String arvorePaginas;

  public Publicacao(Cliente cliente, String versao, String observacao) {
    this.cliente = cliente;
    this.versao = versao;
    this.observacao = observacao;
  }

  public void registrarSucesso(int quantidadePaginas, int quantidadeModulos, String zipNome,
      String zipCaminho, String hashPacote, String relatorioValidacaoJson) {
    this.status = StatusPublicacao.SUCESSO;
    this.quantidadePaginas = quantidadePaginas;
    this.quantidadeModulos = quantidadeModulos;
    this.arquivoZipNome = zipNome;
    this.arquivoZipCaminho = zipCaminho;
    this.hashPacote = hashPacote;
    this.relatorioValidacao = relatorioValidacaoJson;
  }

  public void prepararGeracao() {
    this.status = StatusPublicacao.GERANDO;
    this.quantidadePaginas = 0;
    this.quantidadeModulos = 0;
    this.arquivoZipNome = null;
    this.arquivoZipCaminho = null;
    this.hashPacote = null;
    this.relatorioValidacao = null;
    this.arvorePaginas = null;
  }

  public void definirArvorePaginas(String json) {
    this.arvorePaginas = json;
  }

  public void registrarErro(String mensagem) {
    this.status = StatusPublicacao.ERRO;
    this.observacao = this.observacao == null || this.observacao.isBlank()
        ? mensagem
        : this.observacao + "\nErro: " + mensagem;
  }
}

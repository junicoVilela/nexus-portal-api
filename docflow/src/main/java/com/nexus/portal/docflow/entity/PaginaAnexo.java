package com.nexus.portal.docflow.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tb_pagina_anexo")
public class PaginaAnexo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pagina_id", nullable = false)
  private Pagina pagina;

  @Column(name = "nome_original", nullable = false)
  private String nomeOriginal;

  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @Column(name = "tamanho_bytes", nullable = false)
  private long tamanhoBytes;

  @Column(nullable = false, length = 700)
  private String caminho;

  /** Tela que a captura mostra (INT-601); por padrão, a da página. */
  @Column(name = "codigo_tela", length = 120)
  private String codigoTela;

  /** Região capturada (seletor CSS), para recapturar a mesma área. */
  @Column(length = 300)
  private String seletor;

  public PaginaAnexo(Pagina pagina, String nomeOriginal, String contentType, long tamanhoBytes, String caminho) {
    this.pagina = pagina;
    this.codigoTela = pagina.getCodigoTela();
    this.nomeOriginal = nomeOriginal;
    this.contentType = contentType;
    this.tamanhoBytes = tamanhoBytes;
    this.caminho = caminho;
  }

  /** Vazio volta ao código da página. */
  public void vincularTela(String codigoTela, String seletor) {
    this.codigoTela = codigoTela == null || codigoTela.isBlank() ? pagina.getCodigoTela() : codigoTela.strip();
    this.seletor = seletor == null || seletor.isBlank() ? null : seletor.strip();
  }
}

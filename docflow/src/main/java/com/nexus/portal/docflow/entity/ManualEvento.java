package com.nexus.portal.docflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Uso do manual hospedado (INT-605), sem identificar o leitor. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_manual_evento")
public class ManualEvento {

  public enum Tipo { BUSCA, BUSCA_SEM_RESULTADO, PAGINA_ABERTA }

  @Id
  private UUID id;

  @Column(name = "cliente_id", nullable = false)
  private UUID clienteId;

  @Column(name = "publicacao_id")
  private UUID publicacaoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Tipo tipo;

  @Column(length = 200)
  private String termo;

  @Column(name = "codigo_tela", length = 120)
  private String codigoTela;

  private Integer resultados;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public ManualEvento(UUID clienteId, UUID publicacaoId, Tipo tipo, String termo, String codigoTela, Integer resultados) {
    this.id = UUID.randomUUID();
    this.clienteId = clienteId;
    this.publicacaoId = publicacaoId;
    this.tipo = tipo;
    this.termo = termo;
    this.codigoTela = codigoTela;
    this.resultados = resultados;
  }
}

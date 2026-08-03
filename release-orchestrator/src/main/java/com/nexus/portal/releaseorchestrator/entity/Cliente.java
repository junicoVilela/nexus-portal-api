package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Cliente operacional do Release Orchestrator — o que recebe entregas, contratos
 * de produtos e configurações de ambiente/banco. Distinto do {@code Cliente} do
 * DocFlow (que vive em {@code tb_cliente} e representa o cliente do portal de
 * manuais). Pode ser unificado no futuro via FK pra tb_cliente.
 *
 * Spec: docs/release-orchestrator/03-clientes-cadastro.md
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorCliente")
@Table(name = "tb_cliente_orchestrator")
public class Cliente extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String nome;

  @Column(name = "razao_social", length = 300)
  private String razaoSocial;

  /** CNPJ sem máscara — 14 dígitos quando preenchido. */
  @Column(length = 18)
  private String cnpj;

  @Column(nullable = false, length = 20)
  private String sigla;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "responsavel_comercial_id")
  private UUID responsavelComercialId;

  @Enumerated(EnumType.STRING)
  @Column(name = "ambiente_padrao", nullable = false, length = 20)
  private AmbientePadrao ambientePadrao;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_banco", length = 20)
  private TipoBanco tipoBanco;

  @Column(length = 30)
  private String codificacao;

  @Column(name = "fuso_horario", length = 60)
  private String fusoHorario;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  public Cliente(String nome, String sigla, AmbientePadrao ambientePadrao) {
    this.nome = nome.trim();
    this.sigla = sigla.trim().toUpperCase();
    this.ambientePadrao = ambientePadrao;
    this.ativo = true;
  }

  public void atualizar(String nome, String razaoSocial, String cnpj, String sigla,
      UUID responsavelComercialId, AmbientePadrao ambientePadrao, TipoBanco tipoBanco,
      String codificacao, String fusoHorario, String observacoes) {
    this.nome = nome.trim();
    this.razaoSocial = razaoSocial;
    this.cnpj = cnpj;
    this.sigla = sigla.trim().toUpperCase();
    this.responsavelComercialId = responsavelComercialId;
    this.ambientePadrao = ambientePadrao;
    this.tipoBanco = tipoBanco;
    this.codificacao = codificacao;
    this.fusoHorario = fusoHorario;
    this.observacoes = observacoes;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}

package com.nexus.portal.releaseorchestrator.entity;

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

/**
 * Subset de módulos contratados pelo cliente para um produto + versão atualmente
 * instalada do módulo no cliente. Atualizada ao concluir uma entrega (Fase 1.7+).
 *
 * Spec: docs/release-orchestrator/06-cliente-produtos-contratados.md
 *       docs/release-orchestrator/32-modelo-dados-sugerido.md (ClienteProdutoModulo)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorClienteProdutoModulo")
@Table(name = "tb_cliente_produto_modulo")
public class ClienteProdutoModulo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_produto_id", nullable = false)
  private ClienteProduto clienteProduto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_produto_id", nullable = false)
  private ModuloProduto moduloProduto;

  /** Versão instalada atualmente no cliente. Rótulo livre no MVP. */
  @Column(name = "versao_atual", length = 80)
  private String versaoAtual;

  @Column(nullable = false)
  private boolean ativo = true;

  public ClienteProdutoModulo(ClienteProduto clienteProduto, ModuloProduto moduloProduto,
      String versaoAtual) {
    this.clienteProduto = clienteProduto;
    this.moduloProduto = moduloProduto;
    this.versaoAtual = versaoAtual;
    this.ativo = true;
  }

  public void atualizar(String versaoAtual, boolean ativo) {
    this.versaoAtual = versaoAtual;
    this.ativo = ativo;
  }
}

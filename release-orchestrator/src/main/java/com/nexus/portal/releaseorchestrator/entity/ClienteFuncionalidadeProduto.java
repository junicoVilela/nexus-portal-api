package com.nexus.portal.releaseorchestrator.entity;

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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matriz cliente × funcionalidade do produto (Camada B do catálogo).
 * Granularidade: uma linha por (cliente, funcionalidade). Domínios "possuídos"
 * pelo cliente são derivados via count de habilitada=true por domínio do produto.
 *
 * Spec: docs/release-orchestrator/05-cliente-dominios-funcionalidades.md
 *       docs/release-orchestrator/32-modelo-dados-sugerido.md (ClienteFuncionalidadeProduto)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorClienteFuncionalidadeProduto")
@Table(name = "tb_cliente_funcionalidade_produto")
public class ClienteFuncionalidadeProduto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "funcionalidade_produto_id", nullable = false)
  private FuncionalidadeProduto funcionalidade;

  @Column(nullable = false)
  private boolean habilitada;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrigemFuncionalidade origem;

  public ClienteFuncionalidadeProduto(Cliente cliente, FuncionalidadeProduto funcionalidade,
      boolean habilitada, OrigemFuncionalidade origem) {
    this.cliente = cliente;
    this.funcionalidade = funcionalidade;
    this.habilitada = habilitada;
    this.origem = origem;
  }

  public void atualizar(boolean habilitada, OrigemFuncionalidade origem) {
    this.habilitada = habilitada;
    this.origem = origem;
  }
}

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
 * Módulo de um produto (catálogo livre). Define o que pode ser empacotado
 * em uma release/entrega. `codigo` e `tipo` são imutáveis após criação
 * (regras 7.1 da spec); demais campos via {@link #atualizar}.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_modulo_produto")
public class ModuloProduto extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @Column(nullable = false, length = 80)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoModulo tipo;

  @Column(name = "gera_delta", nullable = false)
  private boolean geraDelta;

  @Column(nullable = false)
  private boolean obrigatorio;

  @Column(nullable = false)
  private int ordem;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "config_especifica", columnDefinition = "TEXT")
  private String configEspecifica;

  public ModuloProduto(ProdutoRh produto, String codigo, String nome, TipoModulo tipo,
      Boolean geraDelta, Boolean obrigatorio, int ordem, String configEspecifica) {
    this.produto = produto;
    this.codigo = codigo;
    this.nome = nome.trim();
    this.tipo = tipo;
    this.geraDelta = geraDelta != null ? geraDelta : tipo.geraDeltaDefault();
    this.obrigatorio = obrigatorio != null ? obrigatorio : tipo.obrigatorioDefault();
    this.ordem = ordem;
    this.ativo = true;
    this.configEspecifica = configEspecifica;
  }

  /**
   * Atualiza campos editáveis. {@code codigo} e {@code tipo} não podem ser
   * alterados depois da criação (ver spec 7.1).
   */
  public void atualizar(String nome, boolean geraDelta, boolean obrigatorio,
      String configEspecifica) {
    this.nome = nome.trim();
    this.geraDelta = geraDelta;
    this.obrigatorio = obrigatorio;
    this.configEspecifica = configEspecifica;
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }

  public void definirOrdem(int ordem) {
    this.ordem = ordem;
  }
}

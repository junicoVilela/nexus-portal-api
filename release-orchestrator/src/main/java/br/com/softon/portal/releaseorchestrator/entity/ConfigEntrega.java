package br.com.softon.portal.releaseorchestrator.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuração de entrega 1:1 com o {@link Cliente} operacional. MVP: só
 * destino PASTA (validado no service).
 *
 * Spec: docs/release-orchestrator/07-cliente-configuracoes-entrega.md
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorConfigEntrega")
@Table(name = "tb_config_entrega_orchestrator")
public class ConfigEntrega extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false, unique = true)
  private Cliente cliente;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_destino", nullable = false, length = 20)
  private TipoDestinoEntrega tipoDestino;

  /** Required quando tipoDestino = PASTA (validado no service). */
  @Column(name = "caminho_base", length = 500)
  private String caminhoBase;

  @Column(name = "exigir_aprovacao", nullable = false)
  private boolean exigirAprovacao;

  /** E-mails separados por vírgula (max 1000 chars). */
  @Column(name = "emails_notificacao", length = 1000)
  private String emailsNotificacao;

  public ConfigEntrega(Cliente cliente, TipoDestinoEntrega tipoDestino, String caminhoBase) {
    this.cliente = cliente;
    this.tipoDestino = tipoDestino;
    this.caminhoBase = caminhoBase;
    this.exigirAprovacao = false;
  }

  public void atualizar(TipoDestinoEntrega tipoDestino, String caminhoBase,
      boolean exigirAprovacao, String emailsNotificacao) {
    this.tipoDestino = tipoDestino;
    this.caminhoBase = caminhoBase;
    this.exigirAprovacao = exigirAprovacao;
    this.emailsNotificacao = emailsNotificacao;
  }
}

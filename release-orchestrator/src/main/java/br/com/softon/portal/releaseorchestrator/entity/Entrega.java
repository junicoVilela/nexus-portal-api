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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrega executada (ou em execução) para um cliente. Agregado central da
 * F1 — gera o pacote local (ZIP), registra metadados (sha256, tamanho) e
 * pode ser reentregue.
 *
 * Estados, transições e fluxos: {@link StatusEntrega}.
 *
 * Spec: docs/release-orchestrator/22-detalhes-entrega.md
 *       docs/release-orchestrator/32-modelo-dados-sugerido.md (Entrega)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorEntrega")
@Table(name = "tb_entrega")
public class Entrega extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  /** Preenchido quando a entrega foi gerada a partir de uma ProximaEntrega. */
  @Column(name = "proxima_entrega_id")
  private UUID proximaEntregaId;

  /** FK para entrega original quando se trata de reentrega (F1.15). */
  @Column(name = "entrega_original_id")
  private UUID entregaOriginalId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AmbientePadrao ambiente;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusEntrega status;

  @Column(name = "data_inicio_geracao")
  private OffsetDateTime dataInicioGeracao;

  @Column(name = "data_conclusao")
  private OffsetDateTime dataConclusao;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  /** Path absoluto do ZIP gerado. Preenchido ao concluir (F1.11/F1.14). */
  @Column(name = "arquivo_pacote_caminho", length = 700)
  private String arquivoPacoteCaminho;

  @Column(name = "arquivo_pacote_sha256", length = 64)
  private String arquivoPacoteSha256;

  @Column(name = "tamanho_bytes")
  private Long tamanhoBytes;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  /** Mensagem de falha quando status = FALHA. */
  @Column(name = "falha_motivo", columnDefinition = "TEXT")
  private String falhaMotivo;

  public Entrega(Cliente cliente, ProdutoRh produto, Release release,
      AmbientePadrao ambiente, UUID responsavelId, String observacoes) {
    this.cliente = cliente;
    this.produto = produto;
    this.release = release;
    this.ambiente = ambiente;
    this.responsavelId = responsavelId;
    this.observacoes = observacoes;
    this.status = StatusEntrega.RASCUNHO;
  }

  public void atualizar(AmbientePadrao ambiente, UUID responsavelId, String observacoes) {
    this.ambiente = ambiente;
    this.responsavelId = responsavelId;
    this.observacoes = observacoes;
  }

  public void alterarStatus(StatusEntrega novo) {
    this.status = novo;
  }

  public void marcarEmGeracao() {
    this.status = StatusEntrega.EM_GERACAO;
    this.dataInicioGeracao = OffsetDateTime.now();
  }

  public void marcarConcluida(String arquivoCaminho, String sha256, long tamanhoBytes) {
    this.status = StatusEntrega.CONCLUIDA;
    this.dataConclusao = OffsetDateTime.now();
    this.arquivoPacoteCaminho = arquivoCaminho;
    this.arquivoPacoteSha256 = sha256;
    this.tamanhoBytes = tamanhoBytes;
  }

  /**
   * Marca que o pacote ZIP foi descartado por retenção (F4 fase 2).
   * O status CONCLUIDA é preservado (histórico intacto); apenas as
   * referências ao arquivo são limpas para que a UI não ofereça download
   * de algo inexistente. {@link #arquivoPacoteSha256} fica como registro
   * de auditoria.
   */
  public void marcarPacoteDescartado() {
    this.arquivoPacoteCaminho = null;
    this.tamanhoBytes = null;
  }

  public void marcarFalha(String motivo) {
    this.status = StatusEntrega.FALHA;
    this.dataConclusao = OffsetDateTime.now();
    this.falhaMotivo = motivo;
  }

  public void marcarCancelada() {
    this.status = StatusEntrega.CANCELADA;
    this.dataConclusao = OffsetDateTime.now();
  }
}

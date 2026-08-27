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
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

  /* --- Publicação remota (F3 P2) --- */

  @Enumerated(EnumType.STRING)
  @Column(name = "status_publicacao", nullable = false, length = 20)
  private StatusPublicacao statusPublicacao = StatusPublicacao.NAO_APLICAVEL;

  @Column(name = "tentativas_publicacao", nullable = false)
  private int tentativasPublicacao;

  @Column(name = "proxima_tentativa_em")
  private OffsetDateTime proximaTentativaEm;

  @Column(name = "ultima_falha_publicacao", columnDefinition = "TEXT")
  private String ultimaFalhaPublicacao;

  @Column(name = "data_publicacao")
  private OffsetDateTime dataPublicacao;

  @Column(name = "destino_publicacao", length = 700)
  private String destinoPublicacao;

  /** Instalações alvo desta entrega (RF-008). Nunca aponta para host. */
  @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EntregaInstalacao> alvos = new ArrayList<>();

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

  public void substituirAlvos(List<InstalacaoCliente> instalacoes) {
    this.alvos.clear();
    if (instalacoes == null) {
      return;
    }
    for (InstalacaoCliente instalacao : instalacoes) {
      this.alvos.add(new EntregaInstalacao(this, instalacao));
    }
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

  /* --- Publicação remota (F3 P2) --- */

  /** Agenda primeira tentativa imediata (chamado quando a geração conclui). */
  public void marcarPublicacaoPendente(OffsetDateTime proximaTentativa) {
    this.statusPublicacao = StatusPublicacao.PENDENTE;
    this.tentativasPublicacao = 0;
    this.proximaTentativaEm = proximaTentativa;
    this.ultimaFalhaPublicacao = null;
    this.dataPublicacao = null;
    this.destinoPublicacao = null;
  }

  /** Sucesso: registra destino e zera agendamento. */
  public void marcarPublicacaoOk(String destino) {
    this.statusPublicacao = StatusPublicacao.OK;
    this.dataPublicacao = OffsetDateTime.now();
    this.destinoPublicacao = destino;
    this.proximaTentativaEm = null;
    this.ultimaFalhaPublicacao = null;
    this.tentativasPublicacao = this.tentativasPublicacao + 1;
  }

  /**
   * Falha temporária: incrementa tentativas e agenda próxima. Caller decide
   * o backoff (geralmente exponencial).
   */
  public void marcarTentativaPublicacaoFalhou(String motivo, OffsetDateTime proximaTentativa) {
    this.statusPublicacao = StatusPublicacao.PENDENTE;
    this.tentativasPublicacao = this.tentativasPublicacao + 1;
    this.proximaTentativaEm = proximaTentativa;
    this.ultimaFalhaPublicacao = motivo;
  }

  /** Esgotou max-tentativas: marca como FALHA definitivo. */
  public void marcarPublicacaoFalhouDefinitivo(String motivo) {
    this.statusPublicacao = StatusPublicacao.FALHA;
    this.tentativasPublicacao = this.tentativasPublicacao + 1;
    this.proximaTentativaEm = null;
    this.ultimaFalhaPublicacao = motivo;
  }

  /** Operador disparou reprocesso manual via UI. */
  public void reagendarPublicacao() {
    this.statusPublicacao = StatusPublicacao.PENDENTE;
    this.tentativasPublicacao = 0;
    this.proximaTentativaEm = OffsetDateTime.now();
    this.ultimaFalhaPublicacao = null;
  }
}

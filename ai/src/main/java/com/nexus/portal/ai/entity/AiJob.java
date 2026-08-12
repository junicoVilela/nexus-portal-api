package com.nexus.portal.ai.entity;

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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_job")
public class AiJob {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sessao_id", nullable = false)
  private AiSessao sessao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiJobTipo tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiJobStatus status = AiJobStatus.PENDENTE;

  @Column(name = "erro_mensagem", columnDefinition = "text")
  private String erroMensagem;

  @Column(name = "erro_detalhe", columnDefinition = "text")
  private String erroDetalhe;

  @Column(name = "diagnostico_id")
  private UUID diagnosticoId;

  @Column(name = "tokens_entrada")
  private Integer tokensEntrada;

  @Column(name = "tokens_saida")
  private Integer tokensSaida;

  @Column(length = 120)
  private String modelo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  private AiJobEtapa etapa = AiJobEtapa.AGUARDANDO;

  @Column(nullable = false)
  private int progresso;

  @Column(nullable = false)
  private int tentativa = 1;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(name = "heartbeat_at")
  private OffsetDateTime heartbeatAt;

  @Column(name = "cancel_requested_at")
  private OffsetDateTime cancelRequestedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public AiJob(AiSessao sessao, AiJobTipo tipo, int tentativa) {
    this.sessao = sessao;
    this.tipo = tipo;
    this.tentativa = Math.max(tentativa, 1);
  }

  public void iniciar(String modelo) {
    this.status = AiJobStatus.PROCESSANDO;
    this.modelo = modelo;
    this.startedAt = OffsetDateTime.now();
    this.heartbeatAt = this.startedAt;
    this.erroMensagem = null;
    this.erroDetalhe = null;
    this.diagnosticoId = null;
  }

  public void atualizarProgresso(AiJobEtapa etapa, int progresso) {
    if (!ativo()) {
      return;
    }
    this.etapa = etapa;
    this.progresso = Math.max(this.progresso, Math.clamp(progresso, 0, 99));
    this.heartbeatAt = OffsetDateTime.now();
  }

  public void registrarTokens(Integer tokensEntrada, Integer tokensSaida) {
    this.tokensEntrada = tokensEntrada;
    this.tokensSaida = tokensSaida;
  }

  public void sucesso() {
    this.status = AiJobStatus.SUCESSO;
    this.etapa = AiJobEtapa.CONCLUIDA;
    this.progresso = 100;
    this.finishedAt = OffsetDateTime.now();
    this.heartbeatAt = this.finishedAt;
  }

  public void erro(String mensagem, String detalhe, UUID diagnosticoId) {
    this.status = AiJobStatus.ERRO;
    this.etapa = AiJobEtapa.FALHA;
    this.erroMensagem = truncar(mensagem == null ? "Erro desconhecido" : mensagem, 2_000);
    this.erroDetalhe = truncar(detalhe, 8_000);
    this.diagnosticoId = diagnosticoId;
    this.finishedAt = OffsetDateTime.now();
    this.heartbeatAt = this.finishedAt;
  }

  public void cancelar() {
    if (!ativo()) {
      return;
    }
    OffsetDateTime agora = OffsetDateTime.now();
    this.status = AiJobStatus.CANCELADO;
    this.etapa = AiJobEtapa.CANCELADA;
    this.cancelRequestedAt = agora;
    this.finishedAt = agora;
    this.heartbeatAt = agora;
  }

  public boolean ativo() {
    return status == AiJobStatus.PENDENTE || status == AiJobStatus.PROCESSANDO;
  }

  public boolean expirado(OffsetDateTime limite) {
    OffsetDateTime referencia = heartbeatAt != null
        ? heartbeatAt
        : startedAt != null ? startedAt : createdAt;
    return ativo() && referencia.isBefore(limite);
  }

  public long latenciaMs() {
    if (startedAt == null || finishedAt == null) {
      return 0L;
    }
    return java.time.Duration.between(startedAt, finishedAt).toMillis();
  }

  private static String truncar(String value, int max) {
    if (value == null || value.length() <= max) {
      return value;
    }
    return value.substring(0, max);
  }
}

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

  @Column(name = "tokens_entrada")
  private Integer tokensEntrada;

  @Column(name = "tokens_saida")
  private Integer tokensSaida;

  @Column(length = 120)
  private String modelo;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public AiJob(AiSessao sessao, AiJobTipo tipo) {
    this.sessao = sessao;
    this.tipo = tipo;
  }

  public void iniciar(String modelo) {
    this.status = AiJobStatus.PROCESSANDO;
    this.modelo = modelo;
    this.startedAt = OffsetDateTime.now();
    this.erroMensagem = null;
  }

  public void registrarTokens(Integer tokensEntrada, Integer tokensSaida) {
    this.tokensEntrada = tokensEntrada;
    this.tokensSaida = tokensSaida;
  }

  public void sucesso() {
    this.status = AiJobStatus.SUCESSO;
    this.finishedAt = OffsetDateTime.now();
  }

  public void erro(String mensagem) {
    this.status = AiJobStatus.ERRO;
    this.erroMensagem = mensagem == null ? "Erro desconhecido" : mensagem;
    if (this.erroMensagem.length() > 2000) {
      this.erroMensagem = this.erroMensagem.substring(0, 2000);
    }
    this.finishedAt = OffsetDateTime.now();
  }

  public long latenciaMs() {
    if (startedAt == null || finishedAt == null) {
      return 0L;
    }
    return java.time.Duration.between(startedAt, finishedAt).toMillis();
  }
}

package com.nexus.portal.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Pergunta ao manual publicado (INT-506). Sem identificar quem perguntou. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_manual_pergunta")
public class AiManualPergunta {

  public enum Origem { PORTAL, LEITOR }

  public enum Modo { IA, TRECHOS, NAO_SEI }

  @Id
  private UUID id;

  @Column(name = "publicacao_id", nullable = false)
  private UUID publicacaoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Origem origem;

  @Column(nullable = false, length = 500)
  private String pergunta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Modo modo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "codigos_citados", nullable = false, columnDefinition = "jsonb")
  private List<String> codigosCitados = List.of();

  @Column(name = "melhor_cobertura", precision = 4, scale = 3)
  private BigDecimal melhorCobertura;

  @Column(name = "latencia_ms", nullable = false)
  private long latenciaMs;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public AiManualPergunta(UUID publicacaoId, Origem origem, String pergunta, Modo modo,
      List<String> codigosCitados, Double melhorCobertura, long latenciaMs) {
    this.id = UUID.randomUUID();
    this.publicacaoId = publicacaoId;
    this.origem = origem;
    this.pergunta = pergunta.length() <= 500 ? pergunta : pergunta.substring(0, 499) + "…";
    this.modo = modo;
    this.codigosCitados = List.copyOf(codigosCitados);
    this.melhorCobertura = melhorCobertura == null ? null
        : BigDecimal.valueOf(Math.min(melhorCobertura, 9.999)).setScale(3, RoundingMode.HALF_UP);
    this.latenciaMs = latenciaMs;
  }
}

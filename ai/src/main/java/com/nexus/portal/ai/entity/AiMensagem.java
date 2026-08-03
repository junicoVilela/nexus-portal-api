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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_ai_mensagem")
public class AiMensagem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sessao_id", nullable = false)
  private AiSessao sessao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AiPapelMensagem papel;

  @Column(nullable = false, columnDefinition = "text")
  private String conteudo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload_json", columnDefinition = "jsonb")
  private String payloadJson;

  @Column(nullable = false)
  private int ordem;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public AiMensagem(AiSessao sessao, AiPapelMensagem papel, String conteudo, String payloadJson, int ordem) {
    this.sessao = sessao;
    this.papel = papel;
    this.conteudo = conteudo;
    this.payloadJson = payloadJson;
    this.ordem = ordem;
  }
}

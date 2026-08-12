package com.nexus.portal.ai.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "tb_ai_documento_importacao")
public class AiDocumentoImportacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "nome_arquivo", nullable = false, length = 255)
  private String nomeArquivo;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_arquivo", nullable = false, length = 10)
  private AiTipoDocumento tipoArquivo;

  @Column(name = "mime_type", nullable = false, length = 160)
  private String mimeType;

  @Column(name = "tamanho_bytes", nullable = false)
  private long tamanhoBytes;

  @Column(name = "hash_sha256", nullable = false, length = 64)
  private String hashSha256;

  @Column(name = "texto_extraido", nullable = false, columnDefinition = "text")
  private String textoExtraido;

  @Column(name = "total_paginas_origem", nullable = false)
  private int totalPaginasOrigem;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AiImportacaoStatus status = AiImportacaoStatus.PRONTO_PARA_REVISAO;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "plano_json", nullable = false, columnDefinition = "jsonb")
  private String planoJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "avisos_json", nullable = false, columnDefinition = "jsonb")
  private String avisosJson;

  @Version
  @Column(nullable = false)
  private long version;

  public AiDocumentoImportacao(
      String nomeArquivo,
      AiTipoDocumento tipoArquivo,
      String mimeType,
      long tamanhoBytes,
      String hashSha256,
      String textoExtraido,
      int totalPaginasOrigem,
      String planoJson,
      String avisosJson) {
    this.nomeArquivo = nomeArquivo;
    this.tipoArquivo = tipoArquivo;
    this.mimeType = mimeType;
    this.tamanhoBytes = tamanhoBytes;
    this.hashSha256 = hashSha256;
    this.textoExtraido = textoExtraido;
    this.totalPaginasOrigem = totalPaginasOrigem;
    this.planoJson = planoJson;
    this.avisosJson = avisosJson;
  }

  public void iniciarRevisao(String planoJson) {
    this.planoJson = planoJson;
    this.status = AiImportacaoStatus.EM_REVISAO;
  }
}

package com.nexus.portal.docflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tb_publicacao_changelog")
public class PublicacaoChangelog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "publicacao_id", nullable = false)
  private UUID publicacaoId;

  @Column(name = "pagina_id")
  private UUID paginaId;

  @Column(name = "pagina_titulo", nullable = false, length = 200)
  private String paginaTitulo;

  @Column(name = "tipo_mudanca", nullable = false, length = 30)
  private String tipoMudanca;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public PublicacaoChangelog(UUID publicacaoId, UUID paginaId, String paginaTitulo, String tipoMudanca) {
    this.publicacaoId = publicacaoId;
    this.paginaId = paginaId;
    this.paginaTitulo = paginaTitulo;
    this.tipoMudanca = tipoMudanca;
  }
}

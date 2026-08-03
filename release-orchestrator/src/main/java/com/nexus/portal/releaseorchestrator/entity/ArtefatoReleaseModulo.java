package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Artefato uploadado manualmente para uma release+módulo (MVP). Uma release pode
 * ter N artefatos por módulo (ex.: múltiplos `.sql` em `BANCO`). O conjunto é
 * imutável após a release ser publicada — service garante a regra.
 *
 * Spec: docs/release-orchestrator/10-produtos-modulos-artefatos.md §3
 *       docs/release-orchestrator/32-modelo-dados-sugerido.md (ArtefatoReleaseModulo)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_artefato_release_modulo")
public class ArtefatoReleaseModulo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_produto_id", nullable = false)
  private ModuloProduto moduloProduto;

  @Column(name = "nome_arquivo", nullable = false, length = 255)
  private String nomeArquivo;

  @Column(name = "caminho_armazenado", nullable = false, length = 700)
  private String caminhoArmazenado;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "tamanho_bytes", nullable = false)
  private long tamanhoBytes;

  @Column(columnDefinition = "TEXT")
  private String observacao;

  public ArtefatoReleaseModulo(Release release, ModuloProduto moduloProduto, String nomeArquivo,
      String caminhoArmazenado, String sha256, long tamanhoBytes, String observacao) {
    this.release = release;
    this.moduloProduto = moduloProduto;
    this.nomeArquivo = nomeArquivo;
    this.caminhoArmazenado = caminhoArmazenado;
    this.sha256 = sha256;
    this.tamanhoBytes = tamanhoBytes;
    this.observacao = observacao;
  }
}

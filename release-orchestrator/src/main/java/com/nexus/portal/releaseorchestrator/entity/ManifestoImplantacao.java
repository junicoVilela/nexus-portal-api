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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Override do manifesto por tipo nesta release. */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorManifestoImplantacao")
@Table(name = "tb_manifesto_implantacao")
public class ManifestoImplantacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_implantacao", nullable = false, length = 30)
  private TipoImplantacao tipoImplantacao;

  @Column(name = "imagem_ref", length = 300)
  private String imagemRef;

  @Column(name = "arquivo_imagem_ref", length = 400)
  private String arquivoImagemRef;

  @Column(name = "diretorio_instalacao", length = 400)
  private String diretorioInstalacao;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  public ManifestoImplantacao(Release release, TipoImplantacao tipoImplantacao) {
    this.release = release;
    this.tipoImplantacao = tipoImplantacao;
  }

  public void atualizar(String imagemRef, String arquivoImagemRef, String diretorioInstalacao,
      String observacoes) {
    this.imagemRef = blankToNull(imagemRef);
    this.arquivoImagemRef = blankToNull(arquivoImagemRef);
    this.diretorioInstalacao = blankToNull(diretorioInstalacao);
    this.observacoes = blankToNull(observacoes);
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}

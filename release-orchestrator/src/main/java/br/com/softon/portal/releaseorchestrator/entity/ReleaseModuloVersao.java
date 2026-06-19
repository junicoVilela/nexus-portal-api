package br.com.softon.portal.releaseorchestrator.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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
 * Vínculo release ↔ versão de cada módulo do produto. No MVP a `versao` é
 * rótulo livre (ex.: "1.5.0"); no pós-MVP passa a ser a tag GitHub real.
 * Uma versão por (release, módulo): UQ enforce.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_release_modulo_versao")
public class ReleaseModuloVersao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "modulo_produto_id", nullable = false)
  private ModuloProduto moduloProduto;

  @Column(nullable = false, length = 80)
  private String versao;

  public ReleaseModuloVersao(Release release, ModuloProduto moduloProduto, String versao) {
    this.release = release;
    this.moduloProduto = moduloProduto;
    this.versao = versao.trim();
  }

  public void alterarVersao(String versao) {
    this.versao = versao.trim();
  }
}

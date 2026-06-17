package br.com.softon.portal.releaseorchestrator.entity;

import br.com.softon.portal.shared.domain.AuditableEntity;
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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_release_item")
public class ReleaseItem extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private CategoriaItem categoria;

  @Column(nullable = false, length = 300)
  private String titulo;

  @Column(columnDefinition = "TEXT")
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VisibilidadeItem visibilidade;

  @Column(nullable = false)
  private int ordem;

  @Column(length = 100)
  private String ticket;

  @Column(name = "commit_hash", length = 100)
  private String commit;

  @Column(name = "pull_request", length = 100)
  private String pullRequest;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  public ReleaseItem(Release release, CategoriaItem categoria, String titulo,
      String descricao, VisibilidadeItem visibilidade, int ordem,
      String ticket, String commit, String pullRequest, UUID responsavelId) {
    this.release = release;
    this.categoria = categoria;
    this.titulo = titulo.trim();
    this.descricao = descricao;
    this.visibilidade = visibilidade != null ? visibilidade : VisibilidadeItem.TODOS;
    this.ordem = ordem;
    this.ticket = ticket;
    this.commit = commit;
    this.pullRequest = pullRequest;
    this.responsavelId = responsavelId;
  }

  public void atualizar(CategoriaItem categoria, String titulo, String descricao,
      VisibilidadeItem visibilidade, String ticket, String commit, String pullRequest,
      UUID responsavelId) {
    this.categoria = categoria;
    this.titulo = titulo.trim();
    this.descricao = descricao;
    this.visibilidade = visibilidade != null ? visibilidade : VisibilidadeItem.TODOS;
    this.ticket = ticket;
    this.commit = commit;
    this.pullRequest = pullRequest;
    this.responsavelId = responsavelId;
  }
}

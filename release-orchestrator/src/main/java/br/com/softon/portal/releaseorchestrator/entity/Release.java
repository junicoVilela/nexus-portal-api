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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_release")
public class Release extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @Column(nullable = false, length = 50)
  private String versao;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoRelease tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReleaseStatus status;

  @Column(name = "data_prevista")
  private LocalDate dataPrevista;

  @Column(name = "data_publicacao")
  private LocalDate dataPublicacao;

  @Column(name = "publicado_por", length = 120)
  private String publicadoPor;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(columnDefinition = "TEXT")
  private String resumo;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  public Release(ProdutoRh produto, String versao, String titulo, TipoRelease tipo,
      ReleaseStatus status, LocalDate dataPrevista, UUID responsavelId,
      String resumo, String observacoes) {
    this.produto = produto;
    this.versao = versao.trim();
    this.titulo = titulo.trim();
    this.tipo = tipo;
    this.status = status;
    this.dataPrevista = dataPrevista;
    this.responsavelId = responsavelId;
    this.resumo = resumo;
    this.observacoes = observacoes;
  }

  public void atualizar(ProdutoRh produto, String versao, String titulo, TipoRelease tipo,
      LocalDate dataPrevista, UUID responsavelId, String resumo, String observacoes) {
    this.produto = produto;
    this.versao = versao.trim();
    this.titulo = titulo.trim();
    this.tipo = tipo;
    this.dataPrevista = dataPrevista;
    this.responsavelId = responsavelId;
    this.resumo = resumo;
    this.observacoes = observacoes;
  }

  public void alterarStatus(ReleaseStatus novoStatus) {
    this.status = novoStatus;
  }

  public void publicar(String publicadoPor) {
    this.status = ReleaseStatus.PUBLICADA;
    this.dataPublicacao = LocalDate.now();
    this.publicadoPor = publicadoPor;
  }

  public void cancelar() {
    this.status = ReleaseStatus.CANCELADA;
  }

  public boolean podeEditar() {
    return status == ReleaseStatus.RASCUNHO || status == ReleaseStatus.EM_DESENVOLVIMENTO;
  }
}

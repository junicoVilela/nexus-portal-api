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

/**
 * Job Jenkins disparado pela ficha da instalação, com cópia para
 * {@code artifacts/} quando o build termina.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tb_build_instalacao_artefato")
public class BuildInstalacaoArtefato extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instalacao_id", nullable = false)
  private InstalacaoCliente instalacao;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modulo_id")
  private ModuloProduto modulo;

  @Column(name = "alvo_id", nullable = false, length = 80)
  private String alvoId;

  @Column(name = "jenkins_job", nullable = false, length = 200)
  private String jenkinsJob;

  @Column(name = "queue_url", length = 500)
  private String queueUrl;

  @Column(name = "build_number")
  private Integer buildNumber;

  @Column(name = "padrao_asset", length = 200)
  private String padraoAsset;

  @Column(name = "nome_arquivo", length = 200)
  private String nomeArquivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusBuildInstalacao status = StatusBuildInstalacao.ENFILEIRADO;

  @Column(columnDefinition = "TEXT")
  private String mensagem;

  public BuildInstalacaoArtefato(InstalacaoCliente instalacao, ProdutoRh produto, ModuloProduto modulo,
      String alvoId, String jenkinsJob, String queueUrl, String padraoAsset, String nomeArquivo) {
    this.instalacao = instalacao;
    this.produto = produto;
    this.modulo = modulo;
    this.alvoId = alvoId;
    this.jenkinsJob = jenkinsJob;
    this.queueUrl = queueUrl;
    this.padraoAsset = padraoAsset;
    this.nomeArquivo = nomeArquivo;
    this.status = StatusBuildInstalacao.ENFILEIRADO;
  }

  public void marcarNumero(int numero) {
    this.buildNumber = numero;
  }

  public void concluir(StatusBuildInstalacao status, String mensagem) {
    this.status = status;
    this.mensagem = mensagem;
  }
}

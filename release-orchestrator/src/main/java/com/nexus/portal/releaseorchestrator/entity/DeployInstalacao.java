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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tentativa de {@code deploy(releaseId, instalacaoId)}. */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorDeployInstalacao")
@Table(name = "tb_deploy_instalacao")
public class DeployInstalacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "release_id", nullable = false)
  private Release release;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instalacao_id", nullable = false)
  private InstalacaoCliente instalacao;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entrega_id")
  private Entrega entrega;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_implantacao", nullable = false, length = 30)
  private TipoImplantacao tipoImplantacao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OperacaoDeploy operacao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ModoDeploy modo = ModoDeploy.DRY_RUN;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusDeploy status = StatusDeploy.PENDENTE;

  @Column(name = "versao_origem", length = 80)
  private String versaoOrigem;

  @Column(name = "versao_destino", nullable = false, length = 80)
  private String versaoDestino;

  @Column(name = "imagem_ref", length = 300)
  private String imagemRef;

  @Column(name = "arquivo_imagem_ref", length = 400)
  private String arquivoImagemRef;

  @Column(name = "diretorio_instalacao", length = 400)
  private String diretorioInstalacao;

  @Column(nullable = false, length = 200)
  private String fingerprint;

  @Column(columnDefinition = "TEXT")
  private String mensagem;

  @Column(columnDefinition = "TEXT")
  private String erro;

  @Column(length = 120)
  private String operador;

  @Column(nullable = false)
  private boolean reutilizado;

  @Column(name = "iniciado_em")
  private OffsetDateTime iniciadoEm;

  @Column(name = "concluido_em")
  private OffsetDateTime concluidoEm;

  public DeployInstalacao(Release release, InstalacaoCliente instalacao, Entrega entrega,
      OperacaoDeploy operacao, String versaoOrigem, String versaoDestino,
      String imagemRef, String arquivoImagemRef, String diretorioInstalacao,
      String fingerprint, String operador) {
    this(release, instalacao, entrega, operacao, ModoDeploy.DRY_RUN, versaoOrigem, versaoDestino,
        imagemRef, arquivoImagemRef, diretorioInstalacao, fingerprint, operador);
  }

  public DeployInstalacao(Release release, InstalacaoCliente instalacao, Entrega entrega,
      OperacaoDeploy operacao, ModoDeploy modo, String versaoOrigem, String versaoDestino,
      String imagemRef, String arquivoImagemRef, String diretorioInstalacao,
      String fingerprint, String operador) {
    this.release = release;
    this.instalacao = instalacao;
    this.entrega = entrega;
    this.tipoImplantacao = instalacao.getTipoImplantacao();
    this.operacao = operacao;
    this.modo = modo == null ? ModoDeploy.DRY_RUN : modo;
    this.status = StatusDeploy.PENDENTE;
    this.versaoOrigem = versaoOrigem;
    this.versaoDestino = versaoDestino;
    this.imagemRef = imagemRef;
    this.arquivoImagemRef = arquivoImagemRef;
    this.diretorioInstalacao = diretorioInstalacao;
    this.fingerprint = fingerprint;
    this.operador = operador;
  }

  public void marcarEmAndamento() {
    this.status = StatusDeploy.EM_ANDAMENTO;
    this.iniciadoEm = OffsetDateTime.now();
  }

  public void marcarConcluido(String mensagem) {
    this.status = StatusDeploy.CONCLUIDO;
    this.mensagem = mensagem;
    this.erro = null;
    this.concluidoEm = OffsetDateTime.now();
  }

  public void marcarFalhou(String erro) {
    this.status = StatusDeploy.FALHA;
    this.erro = erro;
    this.concluidoEm = OffsetDateTime.now();
  }

  public void marcarIgnorado(String mensagem) {
    this.status = StatusDeploy.IGNORADO;
    this.mensagem = mensagem;
    this.reutilizado = true;
    this.concluidoEm = OffsetDateTime.now();
  }
}

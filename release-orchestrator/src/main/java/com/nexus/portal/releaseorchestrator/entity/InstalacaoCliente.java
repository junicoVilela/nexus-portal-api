package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Instalação do produto em um host, para um cliente. Alvo de
 * {@code deploy(releaseId, instalacaoId)} — nunca {@code deploy(..., hostId)}.
 *
 * <p>Spec: automação de implantação por cliente, RF-003 / RF-006 / RF-007.
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "OrchestratorInstalacaoCliente")
@Table(name = "tb_instalacao_cliente")
public class InstalacaoCliente extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 40)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nome;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "host_id", nullable = false)
  private Host host;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "produto_id", nullable = false)
  private ProdutoRh produto;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_implantacao", nullable = false, length = 30)
  private TipoImplantacao tipoImplantacao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusInstalacao status = StatusInstalacao.INEXISTENTE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AmbientePadrao ambiente;

  /** Imagem no registry ({@link TipoImplantacao#DOCKER_PULL}). */
  @Column(name = "imagem_ref", length = 300)
  private String imagemRef;

  /** Arquivo {@code .tar} ({@link TipoImplantacao#DOCKER_TAR}). */
  @Column(name = "arquivo_imagem_ref", length = 400)
  private String arquivoImagemRef;

  /** Diretório no host (instalações manuais Linux/Windows). */
  @Column(name = "diretorio_instalacao", length = 400)
  private String diretorioInstalacao;

  @Column(columnDefinition = "TEXT")
  private String observacoes;

  /** Versão do produto atualmente nesta instalação (RF-006). */
  @Column(name = "versao_atual", length = 80)
  private String versaoAtual;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private HealthInstalacao health = HealthInstalacao.DESCONHECIDO;

  @Column(name = "ultima_verificacao")
  private OffsetDateTime ultimaVerificacao;

  @Column(name = "ultimo_erro", columnDefinition = "TEXT")
  private String ultimoErro;

  @OneToOne(mappedBy = "instalacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private ConfiguracaoInstalacao configuracao;

  @OneToMany(mappedBy = "instalacao", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReservaPorta> portas = new ArrayList<>();

  public InstalacaoCliente(String codigo, String nome, Cliente cliente, Host host, ProdutoRh produto,
      TipoImplantacao tipoImplantacao, AmbientePadrao ambiente) {
    this.codigo = codigo.trim().toUpperCase();
    this.nome = nome.trim();
    this.cliente = cliente;
    this.host = host;
    this.produto = produto;
    this.tipoImplantacao = tipoImplantacao;
    this.ambiente = ambiente;
    this.status = StatusInstalacao.INEXISTENTE;
    this.health = HealthInstalacao.DESCONHECIDO;
  }

  public void atualizar(String codigo, String nome, Cliente cliente, Host host, ProdutoRh produto,
      TipoImplantacao tipoImplantacao, StatusInstalacao status, AmbientePadrao ambiente,
      String imagemRef, String arquivoImagemRef, String diretorioInstalacao, String observacoes) {
    this.codigo = codigo.trim().toUpperCase();
    this.nome = nome.trim();
    this.cliente = cliente;
    this.host = host;
    this.produto = produto;
    this.tipoImplantacao = tipoImplantacao;
    this.status = status;
    this.ambiente = ambiente;
    this.imagemRef = blankToNull(imagemRef);
    this.arquivoImagemRef = blankToNull(arquivoImagemRef);
    this.diretorioInstalacao = blankToNull(diretorioInstalacao);
    this.observacoes = blankToNull(observacoes);
  }

  public void alterarStatus(StatusInstalacao status) {
    this.status = status;
  }

  public void definirVersaoAtual(String versaoAtual) {
    this.versaoAtual = blankToNull(versaoAtual);
  }

  public void registrarHealth(HealthInstalacao health, String versaoAtual, String ultimoErro) {
    this.health = health == null ? HealthInstalacao.DESCONHECIDO : health;
    if (versaoAtual != null) {
      this.versaoAtual = blankToNull(versaoAtual);
    }
    this.ultimoErro = blankToNull(ultimoErro);
    this.ultimaVerificacao = OffsetDateTime.now();
  }

  public void definirConfiguracao(ConfiguracaoInstalacao cfg) {
    if (cfg != null) {
      cfg.setInstalacao(this);
    }
    this.configuracao = cfg;
  }

  public void substituirPortas(List<ReservaPorta> novas) {
    this.portas.clear();
    if (novas == null) {
      return;
    }
    for (ReservaPorta porta : novas) {
      porta.setInstalacao(this);
      porta.setHost(this.host);
      this.portas.add(porta);
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}

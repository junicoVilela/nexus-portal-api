package com.nexus.portal.releaseorchestrator.entity;

import com.nexus.portal.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tb_produto_rh")
public class ProdutoRh extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String nome;

  @Column(nullable = false, length = 20, unique = true)
  private String sigla;

  @Column(length = 500)
  private String descricao;

  @Column(nullable = false, length = 20)
  private String cor;

  @Column(name = "responsavel_id")
  private UUID responsavelId;

  @Column(nullable = false)
  private boolean ativo = true;

  /** owner/repo no GitHub (ex.: nexus/nexus-ld). Nulo desabilita integração. */
  @Column(name = "repositorio_github", length = 200)
  private String repositorioGithub;

  /** Branch base. Default 'main'. */
  @Column(name = "branch_padrao", length = 80)
  private String branchPadrao;

  /** Regex de tags válidas. Default ^v\d+\.\d+\.\d+$. */
  @Column(name = "padrao_tag", length = 200)
  private String padraoTag;

  /** PAT GitHub. MVP texto plano; pós-MVP referência a credencial. */
  @Column(name = "github_token", length = 500)
  private String githubToken;

  /** URL base do Jenkins. Nulo desabilita integração. */
  @Column(name = "jenkins_url", length = 300)
  private String jenkinsUrl;

  @Column(name = "jenkins_job", length = 200)
  private String jenkinsJob;

  @Column(name = "jenkins_user", length = 120)
  private String jenkinsUser;

  /** API token Jenkins. MVP texto plano. */
  @Column(name = "jenkins_token", length = 500)
  private String jenkinsToken;

  @Column(name = "jenkins_trigger_mode", length = 30)
  private String jenkinsTriggerMode;

  public ProdutoRh(String nome, String sigla, String descricao, String cor,
      UUID responsavelId, boolean ativo) {
    this.nome = nome;
    this.sigla = sigla.toUpperCase().trim();
    this.descricao = descricao;
    this.cor = cor;
    this.responsavelId = responsavelId;
    this.ativo = ativo;
  }

  public void atualizar(String nome, String sigla, String descricao, String cor,
      UUID responsavelId, boolean ativo) {
    this.nome = nome;
    this.sigla = sigla.toUpperCase().trim();
    this.descricao = descricao;
    this.cor = cor;
    this.responsavelId = responsavelId;
    this.ativo = ativo;
  }

  public void atualizarIntegracaoGithub(String repositorioGithub, String branchPadrao,
      String padraoTag, String githubToken) {
    this.repositorioGithub = repositorioGithub;
    this.branchPadrao = branchPadrao;
    this.padraoTag = padraoTag;
    if (githubToken != null && !githubToken.isBlank()) {
      this.githubToken = githubToken;
    }
  }

  public boolean temIntegracaoGithub() {
    return repositorioGithub != null && !repositorioGithub.isBlank()
        && githubToken != null && !githubToken.isBlank();
  }

  public void atualizarIntegracaoJenkins(String jenkinsUrl, String jenkinsJob,
      String jenkinsUser, String jenkinsToken, String triggerMode) {
    this.jenkinsUrl = jenkinsUrl;
    this.jenkinsJob = jenkinsJob;
    this.jenkinsUser = jenkinsUser;
    if (jenkinsToken != null && !jenkinsToken.isBlank()) {
      this.jenkinsToken = jenkinsToken;
    }
    this.jenkinsTriggerMode = triggerMode;
  }

  public boolean temIntegracaoJenkins() {
    return jenkinsUrl != null && !jenkinsUrl.isBlank()
        && jenkinsJob != null && !jenkinsJob.isBlank();
  }

  public void alterarStatus(boolean ativo) {
    this.ativo = ativo;
  }
}

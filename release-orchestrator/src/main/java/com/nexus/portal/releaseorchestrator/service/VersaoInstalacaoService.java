package com.nexus.portal.releaseorchestrator.service;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.releaseorchestrator.dto.request.DispararBuildRequest;
import com.nexus.portal.releaseorchestrator.dto.request.OrigemBuild;
import com.nexus.portal.releaseorchestrator.dto.request.ResolverVersaoInstalacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse;
import com.nexus.portal.releaseorchestrator.dto.response.DispararBuildResponse.JobEnfileirado;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse.AlvoBuild;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse.OpcaoVersao;
import com.nexus.portal.releaseorchestrator.dto.response.ResolverVersaoInstalacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.AcaoHistorico;
import com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseHistorico;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog;
import com.nexus.portal.releaseorchestrator.integration.git.LocalGitCatalog.LocalGitRef;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubException;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubRelease;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubTag;
import com.nexus.portal.releaseorchestrator.repository.BuildInstalacaoArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorInstalacaoClienteRepository;
import com.nexus.portal.releaseorchestrator.repository.ProdutoRhRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Escolha de versão na instalação: resolve tag/origem para uma
 * {@link Release} do portal e só então chama
 * {@code deploy(releaseId, instalacaoId)}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersaoInstalacaoService {

  private final OrchestratorInstalacaoClienteRepository instalacaoRepository;
  private final ReleaseRepository releaseRepository;
  private final ReleaseHistoricoRepository historicoRepository;
  private final GitHubReleasesAdapter github;
  private final LocalGitCatalog localGit;
  private final JenkinsBuildService jenkinsBuildService;
  private final BuildAlvoInstalacaoService alvoService;
  private final BuildArtefatoInstalacaoWorker buildWorker;
  private final BuildInstalacaoArtefatoRepository buildArtefatoRepository;
  private final ModuloProdutoRepository moduloRepository;
  private final ProdutoRhRepository produtoRepository;
  private final EscopoResolver escopoResolver;

  @Transactional(readOnly = true)
  public FontesVersaoInstalacaoResponse listarFontes(UUID instalacaoId) {
    // Mesmo critério do GET /instalacoes/{id}: a ficha já autenticou a leitura.
    InstalacaoCliente inst = instalacaoRepository.findById(instalacaoId)
        .orElseThrow(() -> new NotFoundException("Instalação não encontrada."));
    ProdutoRh produto = inst.getProduto();
    String versaoInstalada = inst.getVersaoAtual();
    OpcaoVersao atual = opcaoVersaoAtual(produto);
    OpcaoVersao ultima = null;
    List<OpcaoVersao> tags = List.of();
    String githubErro = null;
    if (jenkinsBuildService.temOrigemGit(produto)) {
      try {
        GitSnapshot snap = carregarOrigemGit(produto);
        ultima = snap.ultima();
        tags = snap.tags();
      } catch (GitHubException e) {
        githubErro = e.getMessage();
      }
    }
    boolean jenkinsOk = produto.temIntegracaoJenkins()
        && produto.getJenkinsToken() != null && !produto.getJenkinsToken().isBlank();
    String aviso = jenkinsOk ? null
        : (!produto.temIntegracaoJenkins()
            ? "Configure Jenkins no produto para gerar o WAR/JAR da versão escolhida."
            : "Configure o API token Jenkins no cadastro do produto.");
    return new FontesVersaoInstalacaoResponse(
        inst.getId(), produto.getId(), versaoInstalada, jenkinsOk, jenkinsBuildService.temOrigemGit(produto),
        produto.getJenkinsJob(), aviso, githubErro, atual, ultima, tags,
        alvoService.listar(inst), alvoService.recentes(inst.getId()));
  }

  @Transactional
  public ResolverVersaoInstalacaoResponse resolver(UUID instalacaoId,
      ResolverVersaoInstalacaoRequest request) {
    InstalacaoCliente inst = buscar(instalacaoId, true);
    OrigemBuild origem = request == null || request.origem() == null
        ? OrigemBuild.RELEASE_ATUAL : request.origem();
    String tagInformada = request == null ? null : request.tag();
    String tag = resolverTag(inst, origem, tagInformada);
    String versao = JenkinsBuildService.versaoPortal(tag);
    ResultadoRelease resultado = garantirRelease(inst.getProduto(), versao, tag, true);
    String aviso = resultado.criada()
        ? "Release " + versao + " cadastrada no portal (em desenvolvimento) para esta implantação."
        : null;
    return new ResolverVersaoInstalacaoResponse(
        resultado.release().getId(), tag, versao, origem.name(), resultado.criada(), aviso);
  }

  @Transactional
  public DispararBuildResponse dispararBuild(UUID instalacaoId, DispararBuildRequest request) {
    InstalacaoCliente inst = buscar(instalacaoId, true);
    OrigemBuild origem = request == null || request.origem() == null
        ? OrigemBuild.RELEASE_ATUAL : request.origem();
    String tagInformada = request == null ? null : request.tag();
    List<AlvoBuild> escolhidos = alvoService.selecionar(
        alvoService.listar(inst),
        request == null ? null : request.alvoIds(),
        inst.getProduto().getId());
    if (escolhidos.isEmpty()) {
      ResolverVersaoInstalacaoResponse resolvida = resolver(instalacaoId,
          new ResolverVersaoInstalacaoRequest(origem, tagInformada));
      return jenkinsBuildService.disparar(resolvida.releaseId(),
          new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, resolvida.tag()));
    }

    Map<String, List<AlvoBuild>> grupos = new LinkedHashMap<>();
    for (AlvoBuild alvo : escolhidos) {
      if (alvo.jenkinsJob() == null || alvo.jenkinsJob().isBlank()) {
        continue;
      }
      grupos.computeIfAbsent(alvo.produtoId() + "|" + alvo.jenkinsJob(), k -> new ArrayList<>()).add(alvo);
    }
    if (grupos.isEmpty()) {
      throw new BusinessException("Nenhum alvo com job Jenkins selecionado.");
    }

    List<JobEnfileirado> jobs = new ArrayList<>();
    List<UUID> pending = new ArrayList<>();
    List<String> avisos = new ArrayList<>();
    UUID firstRelease = null;
    String firstTag = null;
    String firstVersao = null;
    String firstJob = null;
    String firstQueue = null;

    for (List<AlvoBuild> grupo : grupos.values()) {
      AlvoBuild a0 = grupo.get(0);
      ProdutoRh produtoAlvo = produtoRepository.findById(a0.produtoId())
          .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
      String tag;
      try {
        tag = resolverTag(produtoAlvo, origem, tagInformada);
      } catch (BusinessException ex) {
        avisos.add(produtoAlvo.getSigla() + ": " + ex.getMessage());
        continue;
      }
      String versao = JenkinsBuildService.versaoPortal(tag);
      ResultadoRelease rr = garantirRelease(produtoAlvo, versao, tag, true);
      DispararBuildResponse r = jenkinsBuildService.disparar(
          rr.release().getId(),
          new DispararBuildRequest(OrigemBuild.TAG_ESPECIFICA, tag),
          a0.jenkinsJob());
      if (firstRelease == null) {
        firstRelease = r.releaseId();
        firstTag = r.tag();
        firstVersao = r.versao();
        firstJob = r.jenkinsJob();
        firstQueue = r.queueUrl();
      }
      for (AlvoBuild alvo : grupo) {
        ModuloProduto modulo = moduloDe(alvo.id());
        BuildInstalacaoArtefato row = buildArtefatoRepository.save(new BuildInstalacaoArtefato(
            inst, produtoAlvo, modulo, alvo.id(), alvo.jenkinsJob(), r.queueUrl(),
            alvo.padraoAsset(), alvo.nomeArquivo()));
        pending.add(row.getId());
        jobs.add(new JobEnfileirado(
            alvo.id(), alvo.produtoSigla(), alvo.jenkinsJob(), tag, r.queueUrl(), r.aviso()));
      }
    }
    if (jobs.isEmpty()) {
      throw new BusinessException(avisos.isEmpty()
          ? "Não foi possível disparar nenhum job."
          : String.join(" ", avisos));
    }
    String aviso = "Os WAR/JAR entram em artifacts/ quando o Jenkins terminar.";
    if (!avisos.isEmpty()) {
      aviso = aviso + " " + String.join(" ", avisos);
    }
    agendarAcompanhamento(pending);
    return new DispararBuildResponse(
        origem.name(), firstTag, firstVersao, firstRelease, firstRelease,
        firstJob, firstQueue, "EM_ANDAMENTO", aviso, jobs);
  }

  private ModuloProduto moduloDe(String alvoId) {
    if (alvoId == null || !alvoId.startsWith("modulo:")) {
      return null;
    }
    try {
      return moduloRepository.findById(UUID.fromString(alvoId.substring("modulo:".length()))).orElse(null);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private void agendarAcompanhamento(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    List<UUID> copy = List.copyOf(ids);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          buildWorker.acompanhar(copy);
        }
      });
    } else {
      buildWorker.acompanhar(copy);
    }
  }

  private String resolverTag(InstalacaoCliente inst, OrigemBuild origem, String tagInformada) {
    return resolverTag(inst.getProduto(), origem, tagInformada);
  }

  private String resolverTag(ProdutoRh produto, OrigemBuild origem, String tagInformada) {
    return switch (origem) {
      case RELEASE_ATUAL -> {
        String branch = produto.getBranchPadrao();
        if (branch == null || branch.isBlank()) {
          throw new BusinessException(
              "Configure a branch padrão do produto (ex.: v5/main) para usar a versão atual.");
        }
        yield branch.trim();
      }
      case ULTIMA_GERADA -> {
        GitHubRelease ultima = buscarUltimaGerada(produto)
            .orElseThrow(() -> new BusinessException(
                "Não há GitHub Release publicada neste repositório."));
        yield ultima.tagName();
      }
      case TAG_ESPECIFICA -> {
        if (tagInformada == null || tagInformada.isBlank()) {
          throw new BusinessException("Informe a tag ou a versão para instalar.");
        }
        String tag = JenkinsBuildService.tagDaVersao(tagInformada.trim());
        if (!JenkinsBuildService.tagOuBranchPermitida(produto, tag)) {
          throw new BusinessException(
              "Tag '" + tag + "' não combina com o padrão do produto ("
                  + produto.getPadraoTag() + ").");
        }
        yield tag;
      }
    };
  }

  private ResultadoRelease garantirRelease(ProdutoRh produto, String versao, String tag, boolean criar) {
    Optional<Release> existente = releaseRepository.findFirstByProduto_IdAndVersao(produto.getId(), versao);
    if (existente.isPresent()) {
      Release r = existente.get();
      if (r.getStatus() == ReleaseStatus.CANCELADA) {
        throw new BusinessException("A release " + versao + " está cancelada e não pode ser implantada.");
      }
      if (r.getStatus() == ReleaseStatus.RASCUNHO) {
        r.alterarStatus(ReleaseStatus.EM_DESENVOLVIMENTO);
        historicoRepository.save(new ReleaseHistorico(
            r.getId(), AcaoHistorico.EDITADA,
            "Disponibilizada para implantação a partir da instalação.",
            ReleaseStatus.RASCUNHO, ReleaseStatus.EM_DESENVOLVIMENTO, username()));
      }
      if (!ReleaseDisponivelDeployService.IMPLANTAVEIS.contains(r.getStatus())) {
        throw new BusinessException("A release " + versao + " não está em um status implantável.");
      }
      return new ResultadoRelease(r, false);
    }
    if (!criar) {
      throw new BusinessException("Não há release " + versao + " no portal.");
    }
    String titulo = tag == null || tag.isBlank() ? "Versão " + versao : tag;
    Release nova = new Release(produto, versao, titulo, TipoRelease.PATCH,
        ReleaseStatus.EM_DESENVOLVIMENTO, null, null, null, null);
    nova = releaseRepository.save(nova);
    historicoRepository.save(new ReleaseHistorico(
        nova.getId(), AcaoHistorico.CRIADA,
        "Criada automaticamente para implantar " + versao + " nesta instalação.",
        null, ReleaseStatus.EM_DESENVOLVIMENTO, username()));
    log.info("Release {}/{} criada a partir da instalação para deploy.", produto.getSigla(), versao);
    return new ResultadoRelease(nova, true);
  }

  private OpcaoVersao opcaoVersaoAtual(ProdutoRh produto) {
    String branch = produto.getBranchPadrao();
    if (branch == null || branch.isBlank()) {
      return new OpcaoVersao(OrigemBuild.RELEASE_ATUAL.name(), null, null, null, false,
          "Versão atual", "Configure a branch padrão do produto (ex.: v5/main).", null);
    }
    String tag = branch.trim();
    String versao = JenkinsBuildService.versaoPortal(tag);
    Optional<Release> rel = releaseRepository.findFirstByProduto_IdAndVersao(produto.getId(), versao);
    return new OpcaoVersao(OrigemBuild.RELEASE_ATUAL.name(), tag, versao,
        rel.map(Release::getId).orElse(null), true, "Versão atual (" + tag + ")", null, null);
  }

  private Optional<GitHubRelease> buscarUltimaGerada(ProdutoRh produto) {
    if (!jenkinsBuildService.temOrigemGit(produto)) {
      throw new BusinessException(
          "Configure o repositório Git no produto para usar a última release gerada.");
    }
    try {
      OpcaoVersao ultima = carregarOrigemGit(produto).ultima();
      if (ultima == null || ultima.tag() == null) return Optional.empty();
      return Optional.of(new GitHubRelease(
          ultima.tag(), ultima.versao(), false, false, null, List.of()));
    } catch (GitHubException e) {
      throw new BusinessException(e.getMessage());
    }
  }

  private GitSnapshot carregarOrigemGit(ProdutoRh produto) {
    if (produto.temIntegracaoGithub()) {
      return carregarGit(produto);
    }
    if (localGit.temClone(produto.getRepositorioGithub())) {
      return carregarGitLocal(produto);
    }
    return new GitSnapshot(null, List.of());
  }

  private GitSnapshot carregarGitLocal(ProdutoRh produto) {
    List<LocalGitRef> refs = localGit.listarRefs(produto.getRepositorioGithub());
    Map<String, OpcaoVersao> tags = new LinkedHashMap<>();
    for (LocalGitRef r : refs) {
      if (!JenkinsBuildService.tagCombinaPadrao(produto, r.name())) continue;
      tags.putIfAbsent(r.name(), opcaoTag(produto, r.name()));
    }
    OpcaoVersao ultima = comoUltimaGerada(tags);
    return new GitSnapshot(ultima, new ArrayList<>(tags.values()));
  }

  private GitSnapshot carregarGit(ProdutoRh produto) {
    List<GitHubRelease> releases = github.listarReleases(
        produto.getRepositorioGithub(), produto.getGithubToken(), 40, true);
    List<GitHubTag> gitTags = new ArrayList<>();
    List<GitHubTag> tagsApi = github.listarTags(
        produto.getRepositorioGithub(), produto.getGithubToken(), 100);
    if (tagsApi != null) gitTags.addAll(tagsApi);
    for (String prefixo : JenkinsBuildService.prefixosRefsGit(produto.getPadraoTag())) {
      addMatchingRefs(gitTags, produto, prefixo);
    }
    Map<String, OpcaoVersao> tags = new LinkedHashMap<>();
    for (GitHubRelease r : releases) {
      if (r.tagName() == null || r.tagName().isBlank()) continue;
      if (!JenkinsBuildService.tagCombinaPadrao(produto, r.tagName())) continue;
      String versao = JenkinsBuildService.normalizarVersao(r.tagName());
      UUID releaseId = releaseRepository
          .findFirstByProduto_IdAndVersao(produto.getId(), versao)
          .map(Release::getId).orElse(null);
      tags.putIfAbsent(r.tagName(), new OpcaoVersao(OrigemBuild.TAG_ESPECIFICA.name(), r.tagName(),
          versao, releaseId, true,
          r.name() == null || r.name().isBlank() ? r.tagName() : r.name(), null,
          r.assets() == null ? 0 : r.assets().size()));
    }
    for (GitHubTag t : gitTags) {
      if (!JenkinsBuildService.tagCombinaPadrao(produto, t.name())) continue;
      tags.putIfAbsent(t.name(), opcaoTag(produto, t.name()));
    }
    return new GitSnapshot(comoUltimaGerada(tags), new ArrayList<>(tags.values()));
  }

  private OpcaoVersao comoUltimaGerada(Map<String, OpcaoVersao> tags) {
    return tags.values().stream()
        .max(java.util.Comparator.comparing(
            OpcaoVersao::tag, JenkinsBuildService::compararVersao))
        .map(o -> new OpcaoVersao(OrigemBuild.ULTIMA_GERADA.name(), o.tag(), o.versao(),
            o.releaseId(), true, o.titulo(), o.aviso(), o.totalAssets()))
        .orElse(null);
  }

  private void addMatchingRefs(List<GitHubTag> dest, ProdutoRh produto, String prefixo) {
    List<GitHubTag> extra = github.listarMatchingRefs(
        produto.getRepositorioGithub(), produto.getGithubToken(), prefixo);
    if (extra != null) dest.addAll(extra);
  }

  private OpcaoVersao opcaoTag(ProdutoRh produto, String tag) {
    String versao = JenkinsBuildService.normalizarVersao(tag);
    UUID releaseId = releaseRepository
        .findFirstByProduto_IdAndVersao(produto.getId(), versao)
        .map(Release::getId).orElse(null);
    return new OpcaoVersao(OrigemBuild.TAG_ESPECIFICA.name(), tag, versao, releaseId, true,
        tag, null, null);
  }

  private InstalacaoCliente buscar(UUID id, boolean escrever) {
    InstalacaoCliente inst = instalacaoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Instalação não encontrada."));
    if (escrever) {
      escopoResolver.assertPodeEscreverEmCliente(inst.getCliente().getId());
    } else if (!escopoResolver.podeAcessarCliente(inst.getCliente().getId())) {
      throw new NotFoundException("Instalação não encontrada.");
    }
    return inst;
  }

  private String username() {
    var auth = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();
    return auth == null ? "system" : auth.getName();
  }

  private record ResultadoRelease(Release release, boolean criada) {}

  private record GitSnapshot(OpcaoVersao ultima, List<OpcaoVersao> tags) {}
}

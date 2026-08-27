package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ManifestoImplantacaoRequest;
import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ManifestoImplantacao;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorManifestoImplantacaoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolve o manifesto da release para um tipo de implantação. Override
 * cadastrado na release ganha da instalação; campos vazios são derivados.
 */
@Service
@RequiredArgsConstructor
public class ManifestoImplantacaoService {

  private final OrchestratorManifestoImplantacaoRepository repository;
  private final ReleaseRepository releaseRepository;

  public ManifestoImplantacaoResponse resolver(Release release, InstalacaoCliente instalacao) {
    if (!release.getProduto().getId().equals(instalacao.getProduto().getId())) {
      throw new BusinessException("Release e instalação são de produtos diferentes.");
    }
    TipoImplantacao tipo = instalacao.getTipoImplantacao();
    ManifestoImplantacao override = repository
        .findByRelease_IdAndTipoImplantacao(release.getId(), tipo)
        .orElse(null);
    String imagem = first(override == null ? null : override.getImagemRef(),
        comTag(instalacao.getImagemRef(), release.getVersao()),
        padraoImagem(release));
    String tar = first(override == null ? null : override.getArquivoImagemRef(),
        instalacao.getArquivoImagemRef(),
        padraoTar(release));
    String dir = first(override == null ? null : override.getDiretorioInstalacao(),
        instalacao.getDiretorioInstalacao(),
        null);
    boolean derivado = override == null;
    String obs = override == null ? null : override.getObservacoes();
    String resumo = resumir(tipo, release.getVersao(), imagem, tar, dir);
    return new ManifestoImplantacaoResponse(
        release.getId(), release.getVersao(), tipo, imagem, tar, dir, obs, resumo, derivado,
        fingerprint(tipo, release.getVersao(), imagem, tar, dir));
  }

  public List<ManifestoImplantacaoResponse> listarDaRelease(UUID releaseId) {
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    List<ManifestoImplantacaoResponse> out = new ArrayList<>();
    for (TipoImplantacao tipo : TipoImplantacao.values()) {
      ManifestoImplantacao override = repository
          .findByRelease_IdAndTipoImplantacao(releaseId, tipo)
          .orElse(null);
      String imagem = first(override == null ? null : override.getImagemRef(), padraoImagem(release));
      String tar = first(override == null ? null : override.getArquivoImagemRef(), padraoTar(release));
      String dir = override == null ? null : override.getDiretorioInstalacao();
      String resumo = resumir(tipo, release.getVersao(), imagem, tar, dir);
      out.add(new ManifestoImplantacaoResponse(
          release.getId(), release.getVersao(), tipo, imagem, tar, dir,
          override == null ? null : override.getObservacoes(),
          resumo, override == null,
          fingerprint(tipo, release.getVersao(), imagem, tar, dir)));
    }
    return out;
  }

  @Transactional
  public ManifestoImplantacaoResponse salvar(UUID releaseId, ManifestoImplantacaoRequest request) {
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    ManifestoImplantacao entity = repository
        .findByRelease_IdAndTipoImplantacao(releaseId, request.tipoImplantacao())
        .orElseGet(() -> new ManifestoImplantacao(release, request.tipoImplantacao()));
    entity.atualizar(request.imagemRef(), request.arquivoImagemRef(),
        request.diretorioInstalacao(), request.observacoes());
    repository.save(entity);
    String imagem = first(entity.getImagemRef(), padraoImagem(release));
    String tar = first(entity.getArquivoImagemRef(), padraoTar(release));
    String dir = entity.getDiretorioInstalacao();
    return new ManifestoImplantacaoResponse(
        release.getId(), release.getVersao(), entity.getTipoImplantacao(),
        imagem, tar, dir, entity.getObservacoes(),
        resumir(entity.getTipoImplantacao(), release.getVersao(), imagem, tar, dir),
        false,
        fingerprint(entity.getTipoImplantacao(), release.getVersao(), imagem, tar, dir));
  }

  public void validarParaTipo(TipoImplantacao tipo, ManifestoImplantacaoResponse manifesto) {
    switch (tipo) {
      case DOCKER_PULL -> {
        if (blank(manifesto.imagemRef())) {
          throw new BusinessException(
              "Manifesto sem imagem. Informe a imagem na instalação ou no manifesto da release.");
        }
      }
      case DOCKER_TAR -> {
        if (blank(manifesto.arquivoImagemRef())) {
          throw new BusinessException(
              "Manifesto sem arquivo .tar. Informe na instalação ou no manifesto da release.");
        }
      }
      case LINUX_MANUAL, WINDOWS_MANUAL -> {
        if (blank(manifesto.diretorioInstalacao())) {
          throw new BusinessException(
              "Manifesto sem diretório de instalação. Informe na instalação ou no manifesto da release.");
        }
      }
    }
  }

  static String fingerprint(TipoImplantacao tipo, String versao, String imagem, String tar, String dir) {
    return String.join("|",
        tipo.name(),
        n(versao),
        n(imagem),
        n(tar),
        n(dir));
  }

  static String comTag(String imagem, String versao) {
    if (blank(imagem) || blank(versao)) {
      return blank(imagem) ? null : imagem.trim();
    }
    String value = imagem.trim();
    int slash = value.lastIndexOf('/');
    int colon = value.lastIndexOf(':');
    if (colon > slash) {
      return value.substring(0, colon + 1) + versao;
    }
    return value + ":" + versao;
  }

  private static String padraoImagem(Release release) {
    return release.getProduto().getSigla().toLowerCase(Locale.ROOT) + ":" + release.getVersao();
  }

  private static String padraoTar(Release release) {
    return release.getProduto().getSigla().toLowerCase(Locale.ROOT) + "-" + release.getVersao() + ".tar";
  }

  private static String resumir(TipoImplantacao tipo, String versao, String imagem, String tar, String dir) {
    return switch (tipo) {
      case DOCKER_PULL -> "Docker pull " + n(imagem) + " (versão " + versao + ")";
      case DOCKER_TAR -> "Docker load " + n(tar) + " (versão " + versao + ")";
      case LINUX_MANUAL -> "Linux manual em " + n(dir) + " (versão " + versao + ")";
      case WINDOWS_MANUAL -> "Windows manual em " + n(dir) + " (versão " + versao + ")";
    };
  }

  private static String first(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (!blank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String n(String value) {
    return blank(value) ? "-" : value.trim();
  }
}

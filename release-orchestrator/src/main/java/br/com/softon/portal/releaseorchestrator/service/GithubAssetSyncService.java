package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubException;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubRelease;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import br.com.softon.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sincroniza assets de Releases do GitHub como {@link ArtefatoReleaseModulo}
 * persistidos no portal. Usado pela geração quando o módulo é WEB/BATCH e o
 * produto tem integração GitHub configurada, evitando upload manual (F2.1).
 *
 * <p>Política:
 * <ul>
 *   <li>Se já há artefatos persistidos para a (release, módulo), nada é feito
 *       — o cache vale para todas as entregas que usam essa release.</li>
 *   <li>Tag GitHub é deduzida do {@code release.versao} prefixado com 'v'.</li>
 *   <li>Asset é filtrado por extensão usando
 *       {@link TipoModulo#extensoesAceitasDefault()}.</li>
 *   <li>Falhas (404 da tag, token inválido, etc.) lançam
 *       {@link GitHubException}; caller decide se quebra a geração ou continua
 *       com lista vazia (delta vazio).</li>
 * </ul>
 *
 * <p>Apenas módulos {@link TipoModulo#aceitaUploadDeArtefato()} são candidatos
 * — FUNCIONALIDADES/REGRAS são gerados em runtime, não baixados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubAssetSyncService {

  private final GitHubReleasesAdapter adapter;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final ReleaseOrchestratorStorageProperties storage;

  /**
   * Garante que existem artefatos persistidos para (release, módulo) no
   * banco. Se a base já tem artefatos, retorna eles. Se não, tenta baixar
   * do GitHub. Retorna lista vazia quando não há integração ou nenhum asset
   * combina com as extensões aceitas.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ArtefatoReleaseModulo> sincronizar(Release release, ModuloProduto modulo) {
    List<ArtefatoReleaseModulo> existentes = artefatoRepository
        .findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(release.getId(), modulo.getId());
    if (!existentes.isEmpty()) return existentes;

    if (!modulo.getTipo().aceitaUploadDeArtefato()) return existentes;

    ProdutoRh produto = release.getProduto();
    if (produto == null || !produto.temIntegracaoGithub()) return existentes;

    String tag = "v" + release.getVersao();
    GitHubRelease ghRelease = adapter
        .buscarPorTag(produto.getRepositorioGithub(), tag, produto.getGithubToken())
        .orElse(null);
    if (ghRelease == null) {
      log.info("Release GitHub {}/{} ausente — nenhum asset para baixar.",
          produto.getRepositorioGithub(), tag);
      return existentes;
    }

    List<String> extensoes = modulo.getTipo().extensoesAceitasDefault();
    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    for (GitHubRelease.Asset asset : ghRelease.assets()) {
      if (!casaExtensao(asset.name(), extensoes)) continue;
      try {
        ArtefatoReleaseModulo art = baixarECriarArtefato(release, modulo, asset, produto);
        criados.add(art);
      } catch (IOException | RuntimeException e) {
        log.warn("Falha ao baixar asset {} para release {}/{} módulo {}: {}",
            asset.name(), produto.getRepositorioGithub(), tag, modulo.getCodigo(),
            e.getMessage());
      }
    }
    return criados;
  }

  private ArtefatoReleaseModulo baixarECriarArtefato(Release release, ModuloProduto modulo,
      GitHubRelease.Asset asset, ProdutoRh produto) throws IOException {
    Path destinoDir = Path.of(storage.artefatosDir(),
        "github-cache",
        release.getProduto().getSigla().toLowerCase(),
        release.getId().toString(),
        modulo.getId().toString());
    Files.createDirectories(destinoDir);
    Path destino = destinoDir.resolve(asset.name());

    MessageDigest digest = sha256();
    try (InputStream in = adapter.baixarAsset(
            produto.getRepositorioGithub(), asset.id(), produto.getGithubToken());
        DigestInputStream dig = new DigestInputStream(in, digest)) {
      Files.copy(dig, destino, StandardCopyOption.REPLACE_EXISTING);
    }
    String hash = HexFormat.of().formatHex(digest.digest());
    long tamanho = Files.size(destino);

    var art = new ArtefatoReleaseModulo(release, modulo, asset.name(),
        destino.toAbsolutePath().toString(), hash, tamanho,
        "Baixado automaticamente do GitHub Release " + release.getVersao());
    return artefatoRepository.save(art);
  }

  private boolean casaExtensao(String nomeArquivo, List<String> extensoes) {
    if (nomeArquivo == null || extensoes.isEmpty()) return false;
    String lower = nomeArquivo.toLowerCase();
    return extensoes.stream().anyMatch(lower::endsWith);
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
    }
  }
}

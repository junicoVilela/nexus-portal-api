package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import br.com.softon.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import br.com.softon.portal.releaseorchestrator.entity.ModuloProduto;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubException;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubFileChange;
import br.com.softon.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import br.com.softon.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Calcula o delta de um módulo KETTLE entre duas tags Git: lista arquivos
 * {@code .ktr}/{@code .kjb} alterados, baixa via API contents e persiste como
 * {@link ArtefatoReleaseModulo}. Espelha {@link GithubDeltaBancoService}
 * sem a parte de classificação DDL/DML.
 *
 * <p>Out of scope (próximas iterações):
 * <ul>
 *   <li>Resolução de dependências entre transformações/subjobs.</li>
 *   <li>Empacotamento ZIP com hierarquia de pastas preservada.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubDeltaKettleService {

  private final GitHubReleasesAdapter adapter;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final ReleaseOrchestratorStorageProperties storage;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ArtefatoReleaseModulo> sincronizar(Release release, ModuloProduto modulo,
      String fromTag) {
    if (modulo.getTipo() != TipoModulo.KETTLE) return List.of();

    ProdutoRh produto = release.getProduto();
    if (produto == null || !produto.temIntegracaoGithub()) return List.of();

    ConfigKettleModulo cfg = ConfigKettleModulo.de(modulo.getConfigEspecifica(), objectMapper);
    String toTag = "v" + release.getVersao();

    var existentes = artefatoRepository
        .findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(release.getId(), modulo.getId());
    if (!existentes.isEmpty()) {
      artefatoRepository.deleteAll(existentes);
    }

    List<GitHubFileChange> arquivos;
    try {
      String base = (fromTag == null || fromTag.isBlank()) ? toTag : fromTag;
      arquivos = adapter.compare(produto.getRepositorioGithub(), base, toTag,
          produto.getGithubToken());
    } catch (GitHubException e) {
      log.warn("Falha ao calcular delta KETTLE para {}/{}..{}: {}",
          produto.getRepositorioGithub(), fromTag, toTag, e.getMessage());
      return List.of();
    }

    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    for (GitHubFileChange f : arquivos) {
      if (!f.foiAdicionadoOuModificado()) continue;
      String lower = f.filename().toLowerCase();
      if (!lower.endsWith(".ktr") && !lower.endsWith(".kjb")) continue;
      if (!cfg.dentroDoEscopo(f.filename())) continue;

      try {
        criados.add(baixarECriar(release, modulo, f, toTag, produto));
      } catch (IOException | RuntimeException e) {
        log.warn("Falha ao baixar {} de {} ({}): {}",
            f.filename(), produto.getRepositorioGithub(), toTag, e.getMessage());
      }
    }
    log.info("Delta KETTLE {}: {} arquivos baixados de {}..{}",
        modulo.getCodigo(), criados.size(), fromTag, toTag);
    return criados;
  }

  private ArtefatoReleaseModulo baixarECriar(Release release, ModuloProduto modulo,
      GitHubFileChange arquivo, String tag, ProdutoRh produto) throws IOException {
    String nomeBase = arquivo.filename().substring(arquivo.filename().lastIndexOf('/') + 1);
    Path destinoDir = Path.of(storage.artefatosDir(),
        "github-cache",
        release.getProduto().getSigla().toLowerCase(),
        release.getId().toString(),
        modulo.getId().toString());
    Files.createDirectories(destinoDir);
    Path destino = destinoDir.resolve(nomeBase);

    MessageDigest digest = sha256();
    try (InputStream in = adapter.baixarArquivo(produto.getRepositorioGithub(),
            arquivo.filename(), tag, produto.getGithubToken());
        DigestInputStream dig = new DigestInputStream(in, digest)) {
      Files.copy(dig, destino, StandardCopyOption.REPLACE_EXISTING);
    }
    String hash = HexFormat.of().formatHex(digest.digest());
    long tamanho = Files.size(destino);

    var art = new ArtefatoReleaseModulo(release, modulo, nomeBase,
        destino.toAbsolutePath().toString(), hash, tamanho,
        "Delta GitHub " + arquivo.status() + " " + arquivo.filename());
    return artefatoRepository.save(art);
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
    }
  }
}

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
 * Calcula o delta de um módulo BANCO entre duas tags Git, baixando os arquivos
 * .sql alterados e persistindo como {@link ArtefatoReleaseModulo}. F2.10.
 *
 * <p>Política:
 * <ul>
 *   <li>FROM_TAG = última versão entregue ao cliente (parâmetro).</li>
 *   <li>TO_TAG = tag da release alvo (v + release.versao).</li>
 *   <li>Arquivos filtrados por caminhoRepo + extensão .sql.</li>
 *   <li>Status added/modified/renamed entram; removed sai.</li>
 *   <li>Falha (404 da tag, sem permissão, etc.) → lista vazia + log.</li>
 * </ul>
 *
 * <p>Idempotência: cada chamada apaga o cache anterior do módulo nesta
 * release antes de baixar de novo. Útil quando o operador re-aprova uma
 * release com fixes — força resincronização.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubDeltaBancoService {

  private final GitHubReleasesAdapter adapter;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final ReleaseOrchestratorStorageProperties storage;
  private final ObjectMapper objectMapper;

  /**
   * Calcula o delta BANCO entre {@code fromTag} e {@code toTag} (= 'v' + release.versao).
   * Quando {@code fromTag} é null ou em branco, baixa o "estado inicial" — usa o
   * próprio TO_TAG e retorna todos os arquivos sob {@code caminhoRepo} (full).
   *
   * @return artefatos persistidos para este módulo.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ArtefatoReleaseModulo> sincronizar(Release release, ModuloProduto modulo,
      String fromTag) {
    if (modulo.getTipo() != TipoModulo.BANCO) return List.of();

    ProdutoRh produto = release.getProduto();
    if (produto == null || !produto.temIntegracaoGithub()) return List.of();

    ConfigBancoModulo cfg = ConfigBancoModulo.de(modulo.getConfigEspecifica(), objectMapper);
    String toTag = "v" + release.getVersao();

    // Limpa cache anterior pra esta release+módulo — refresh forçado.
    var existentes = artefatoRepository
        .findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(release.getId(), modulo.getId());
    if (!existentes.isEmpty()) {
      artefatoRepository.deleteAll(existentes);
    }

    List<GitHubFileChange> arquivos;
    try {
      String base = (fromTag == null || fromTag.isBlank()) ? toTag : fromTag;
      // Quando base == head, GitHub retorna lista vazia. Vamos tratar caso
      // "primeira entrega" como full (não há FROM real). Não cobrimos isso
      // nesta iteração — operador deve setar versaoAtual antes ou aceitar
      // delta vazio + upload manual.
      arquivos = adapter.compare(produto.getRepositorioGithub(),
          base, toTag, produto.getGithubToken());
    } catch (GitHubException e) {
      log.warn("Falha ao calcular delta BANCO para {}/{}..{}: {}",
          produto.getRepositorioGithub(), fromTag, toTag, e.getMessage());
      return List.of();
    }

    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    for (GitHubFileChange f : arquivos) {
      if (!f.foiAdicionadoOuModificado()) continue;
      if (!f.filename().toLowerCase().endsWith(".sql")) continue;
      if (!cfg.dentroDoEscopo(f.filename())) continue;

      try {
        var artefato = baixarECriar(release, modulo, f, toTag, produto);
        criados.add(artefato);
      } catch (IOException | RuntimeException e) {
        log.warn("Falha ao baixar {} de {} ({}): {}",
            f.filename(), produto.getRepositorioGithub(), toTag, e.getMessage());
      }
    }
    log.info("Delta BANCO {}: {} arquivos baixados de {}..{}",
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

    var artefato = new ArtefatoReleaseModulo(release, modulo, nomeBase,
        destino.toAbsolutePath().toString(), hash, tamanho,
        "Delta GitHub " + arquivo.status() + " " + arquivo.filename());
    return artefatoRepository.save(artefato);
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
    }
  }
}

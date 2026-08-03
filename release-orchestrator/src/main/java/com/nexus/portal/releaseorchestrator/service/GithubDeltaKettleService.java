package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.config.ReleaseOrchestratorStorageProperties;
import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubException;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubFileChange;
import com.nexus.portal.releaseorchestrator.integration.github.GitHubReleasesAdapter;
import com.nexus.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula o delta de um módulo KETTLE entre duas tags Git: lista arquivos
 * {@code .ktr}/{@code .kjb} alterados, baixa via API contents e persiste como
 * {@link ArtefatoReleaseModulo}. Quando {@link ConfigKettleModulo#incluirDependencias()}
 * está ativo, parseia cada arquivo baixado e inclui transformações/jobs
 * referenciados (resolução em profundidade arbitrária com proteção contra
 * ciclos).
 *
 * <p>O nome do artefato preserva o path relativo dentro do {@code caminhoRepo}
 * — assim o empacotador final reconstrói a hierarquia esperada pelo Kettle
 * no cliente (jobs encontram seus subjobs).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubDeltaKettleService {

  private final GitHubReleasesAdapter adapter;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final ReleaseOrchestratorStorageProperties storage;
  private final ObjectMapper objectMapper;
  private final KettleDependencyParser parser;

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

    // Conjunto de paths já agendados pra evitar ciclos e duplicatas.
    Set<String> processados = new HashSet<>();
    Deque<String> pendentes = new ArrayDeque<>();

    // Sementes: arquivos do delta que casam com extensão e caminhoRepo.
    for (GitHubFileChange f : arquivos) {
      if (!f.foiAdicionadoOuModificado()) continue;
      if (!ehArquivoKettle(f.filename())) continue;
      if (!cfg.dentroDoEscopo(f.filename())) continue;
      pendentes.offer(f.filename());
    }

    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    while (!pendentes.isEmpty()) {
      String path = pendentes.poll();
      if (!processados.add(path)) continue;
      try {
        ArtefatoReleaseModulo artefato = baixarECriar(release, modulo, path, toTag, produto);
        criados.add(artefato);
        if (cfg.incluirDependencias()) {
          Set<String> deps = extrairDependencias(artefato);
          for (String ref : deps) {
            String resolvido = parser.resolver(ref, path);
            if (resolvido == null || resolvido.isBlank()) continue;
            if (resolvido.startsWith("..")) {
              log.debug("Dep {} sai do repo após resolver — ignorada.", ref);
              continue;
            }
            if (!processados.contains(resolvido)) {
              pendentes.offer(resolvido);
            }
          }
        }
      } catch (IOException | RuntimeException e) {
        log.warn("Falha ao baixar {} de {} ({}): {}",
            path, produto.getRepositorioGithub(), toTag, e.getMessage());
      }
    }
    log.info("Delta KETTLE {}: {} arquivos (deps={}) de {}..{}",
        modulo.getCodigo(), criados.size(), cfg.incluirDependencias(), fromTag, toTag);
    return criados;
  }

  private boolean ehArquivoKettle(String filename) {
    String lower = filename.toLowerCase();
    return lower.endsWith(".ktr") || lower.endsWith(".kjb");
  }

  /**
   * Lê o arquivo já salvo em disco e extrai referências literais a outros
   * .ktr/.kjb. Reutiliza o cache local para evitar segunda chamada GitHub.
   */
  private Set<String> extrairDependencias(ArtefatoReleaseModulo art) {
    Path arquivo = Path.of(art.getCaminhoArmazenado());
    try (InputStream in = Files.newInputStream(arquivo)) {
      return parser.extrair(in);
    } catch (IOException e) {
      log.warn("Não consegui ler {} para extrair deps: {}", arquivo, e.getMessage());
      return new LinkedHashSet<>();
    }
  }

  private ArtefatoReleaseModulo baixarECriar(Release release, ModuloProduto modulo,
      String filenameRepo, String tag, ProdutoRh produto) throws IOException {
    String nomeBase = filenameRepo.substring(filenameRepo.lastIndexOf('/') + 1);
    Path destinoDir = Path.of(storage.artefatosDir(),
        "github-cache",
        release.getProduto().getSigla().toLowerCase(),
        release.getId().toString(),
        modulo.getId().toString(),
        pastaRelativa(filenameRepo));
    Files.createDirectories(destinoDir);
    Path destino = destinoDir.resolve(nomeBase);

    MessageDigest digest = sha256();
    try (InputStream in = adapter.baixarArquivo(produto.getRepositorioGithub(),
            filenameRepo, tag, produto.getGithubToken());
        DigestInputStream dig = new DigestInputStream(in, digest)) {
      Files.copy(dig, destino, StandardCopyOption.REPLACE_EXISTING);
    }
    String hash = HexFormat.of().formatHex(digest.digest());
    long tamanho = Files.size(destino);

    // Nome do artefato preserva o path relativo ao caminhoRepo para que o
    // empacotador final reconstrua a hierarquia no ZIP.
    String nomeArtefato = filenameRepo;
    var art = new ArtefatoReleaseModulo(release, modulo, nomeArtefato,
        destino.toAbsolutePath().toString(), hash, tamanho,
        "Delta GitHub kettle " + filenameRepo);
    return artefatoRepository.save(art);
  }

  private String pastaRelativa(String filenameRepo) {
    int barra = filenameRepo.lastIndexOf('/');
    return barra >= 0 ? filenameRepo.substring(0, barra) : "";
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
    }
  }

  /** Apenas para testes manuais. */
  @SuppressWarnings("unused")
  private Set<String> extrairDependenciasDeBytes(byte[] bytes) {
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      return parser.extrair(in);
    } catch (IOException e) {
      return new LinkedHashSet<>();
    }
  }
}

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
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula o delta de um módulo BANCO entre duas tags Git e produz arquivos
 * unificados — {@code DDL.sql} e {@code DML.sql} — por dialeto configurado
 * em {@link ConfigBancoModulo}. F2.10 + spec 20 §5.2.
 *
 * <p>Mono-dialeto (config legada): artefatos saem na raiz como
 * {@code DDL.sql} / {@code DML.sql}.
 *
 * <p>Multi-dialeto: artefatos saem em subpastas nomeadas pelo dialeto:
 * {@code oracle/DDL.sql}, {@code sqlserver/DDL.sql} etc. O empacotador do
 * pacote final honra os {@code /} no {@code nomeArquivo} e preserva a
 * hierarquia no ZIP.
 *
 * <p>Política:
 * <ul>
 *   <li>FROM_TAG = última versão entregue ao cliente (parâmetro).</li>
 *   <li>TO_TAG = tag da release alvo (v + release.versao).</li>
 *   <li>Compare API chamado uma vez; resultado particionado por dialeto.</li>
 *   <li>Arquivos filtrados por caminhoRepo (do dialeto) + extensão .sql.</li>
 *   <li>Status added/modified/renamed entram; removed sai.</li>
 *   <li>Ordenação alfabética por nome do arquivo dentro de cada bloco.</li>
 *   <li>Arquivos que não casam com prefixoDDL nem prefixoDML são ignorados
 *       (log warn).</li>
 *   <li>Apenas blocos não-vazios viram artefato.</li>
 * </ul>
 *
 * <p>Idempotência: cada chamada apaga o cache anterior do módulo nesta
 * release antes de baixar de novo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubDeltaBancoService {

  private final GitHubReleasesAdapter adapter;
  private final ArtefatoReleaseModuloRepository artefatoRepository;
  private final ReleaseOrchestratorStorageProperties storage;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<ArtefatoReleaseModulo> sincronizar(Release release, ModuloProduto modulo,
      String fromTag) {
    if (modulo.getTipo() != TipoModulo.BANCO) return List.of();

    ProdutoRh produto = release.getProduto();
    if (produto == null || !produto.temIntegracaoGithub()) return List.of();

    ConfigBancoModulo cfg = ConfigBancoModulo.de(modulo.getConfigEspecifica(), objectMapper);
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
      log.warn("Falha ao calcular delta BANCO para {}/{}..{}: {}",
          produto.getRepositorioGithub(), fromTag, toTag, e.getMessage());
      return List.of();
    }

    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    try {
      Path baseDir = preparaDir(release, modulo);
      for (ConfigBancoModulo.Dialeto dialeto : cfg.dialetosResolvidos()) {
        criados.addAll(processarDialeto(release, modulo, produto, toTag,
            baseDir, cfg, dialeto, arquivos));
      }
    } catch (IOException | RuntimeException e) {
      log.warn("Falha ao concatenar delta BANCO {}/{}..{}: {}",
          produto.getRepositorioGithub(), fromTag, toTag, e.getMessage());
    }
    return criados;
  }

  private List<ArtefatoReleaseModulo> processarDialeto(Release release, ModuloProduto modulo,
      ProdutoRh produto, String toTag, Path baseDir, ConfigBancoModulo cfg,
      ConfigBancoModulo.Dialeto dialeto, List<GitHubFileChange> arquivos) throws IOException {

    List<GitHubFileChange> ddl = new ArrayList<>();
    List<GitHubFileChange> dml = new ArrayList<>();
    for (GitHubFileChange f : arquivos) {
      if (!f.foiAdicionadoOuModificado()) continue;
      if (!f.filename().toLowerCase().endsWith(".sql")) continue;
      if (!cfg.dentroDoEscopo(f.filename(), dialeto)) continue;

      String nomeBase = nomeBase(f.filename());
      if (cfg.ehDDL(nomeBase)) {
        ddl.add(f);
      } else if (cfg.ehDML(nomeBase)) {
        dml.add(f);
      } else {
        log.warn("Arquivo {} não casa com prefixos DDL/DML — ignorado.", f.filename());
      }
    }

    Comparator<GitHubFileChange> byName =
        Comparator.comparing(f -> nomeBase(f.filename()).toLowerCase());
    ddl.sort(byName);
    dml.sort(byName);

    Path dialetoDir = dialeto.ehNomeado()
        ? Files.createDirectories(baseDir.resolve(dialeto.nome()))
        : baseDir;

    List<ArtefatoReleaseModulo> criados = new ArrayList<>();
    if (!ddl.isEmpty()) {
      criados.add(concatenarESalvar(release, modulo, produto, toTag, dialetoDir,
          nomeArtefato(dialeto, "DDL.sql"), ddl));
    }
    if (!dml.isEmpty()) {
      criados.add(concatenarESalvar(release, modulo, produto, toTag, dialetoDir,
          nomeArtefato(dialeto, "DML.sql"), dml));
    }
    log.info("Delta BANCO {} [{}]: DDL={} DML={}",
        modulo.getCodigo(), dialeto.ehNomeado() ? dialeto.nome() : "default",
        ddl.size(), dml.size());
    return criados;
  }

  /**
   * Baixa todos os arquivos do bloco em streaming e escreve num único
   * arquivo de saída, com cabeçalho por script para preservar a procedência.
   */
  private ArtefatoReleaseModulo concatenarESalvar(Release release, ModuloProduto modulo,
      ProdutoRh produto, String tag, Path dir, String nomeArtefato,
      List<GitHubFileChange> arquivos) throws IOException {
    String nomeSimples = nomeBase(nomeArtefato);
    Path saida = dir.resolve(nomeSimples);
    MessageDigest digest = sha256();
    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(saida));
        DigestOutputStream dig = new DigestOutputStream(out, digest)) {
      for (GitHubFileChange f : arquivos) {
        String nomeBase = nomeBase(f.filename());
        String header = "-- ============================================================\n"
            + "-- " + nomeBase + "\n"
            + "-- origem: " + f.filename() + " (" + f.status() + ")\n"
            + "-- ============================================================\n";
        dig.write(header.getBytes(StandardCharsets.UTF_8));
        try (InputStream in = adapter.baixarArquivo(produto.getRepositorioGithub(),
            f.filename(), tag, produto.getGithubToken())) {
          in.transferTo(dig);
        }
        dig.write("\n\n".getBytes(StandardCharsets.UTF_8));
      }
    }

    String hash = HexFormat.of().formatHex(digest.digest());
    long tamanho = Files.size(saida);
    String observacao = "Delta GitHub concatenado: " + arquivos.size() + " arquivos";
    var artefato = new ArtefatoReleaseModulo(release, modulo, nomeArtefato,
        saida.toAbsolutePath().toString(), hash, tamanho, observacao);
    return artefatoRepository.save(artefato);
  }

  private String nomeArtefato(ConfigBancoModulo.Dialeto dialeto, String fileName) {
    return dialeto.ehNomeado() ? dialeto.nome() + "/" + fileName : fileName;
  }

  private Path preparaDir(Release release, ModuloProduto modulo) throws IOException {
    Path destino = Path.of(storage.artefatosDir(),
        "github-cache",
        release.getProduto().getSigla().toLowerCase(),
        release.getId().toString(),
        modulo.getId().toString());
    Files.createDirectories(destino);
    return destino;
  }

  private String nomeBase(String filename) {
    int barra = filename.lastIndexOf('/');
    return barra >= 0 ? filename.substring(barra + 1) : filename;
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
    }
  }
}

package br.com.softon.portal.releaseorchestrator.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Extrai referências a outras transformações/jobs (`.ktr`/`.kjb`) de um
 * arquivo Kettle (PDI). Usado pelo {@link GithubDeltaKettleService} quando
 * {@code incluirDependencias=true}.
 *
 * <p>Estratégia: scanning regex por padrões comuns em XML do Kettle:
 * <ul>
 *   <li>{@code <filename>...</filename>} dentro de entries de job, mappings
 *       de transformações, e steps "Transformation Executor" / "Job Executor".</li>
 *   <li>{@code <transname>...</transname>}, {@code <jobname>...</jobname>}
 *       em conjunto com {@code <directory>}.</li>
 * </ul>
 *
 * <p>Limitações conhecidas:
 * <ul>
 *   <li>Caminhos com variáveis Kettle ({@code ${PDI_HOME}/x.ktr}) são
 *       ignorados — não há como resolver sem o ambiente real.</li>
 *   <li>Profundidade fixa: caller resolve apenas 1 nível por chamada.
 *       Para profundidade arbitrária, caller chama recursivo.</li>
 *   <li>Caminhos absolutos do sistema (começando com {@code /}) também são
 *       ignorados — provavelmente apontam pra fora do repo.</li>
 * </ul>
 *
 * <p>SAX/DOM seria mais robusto mas o XML do Kettle tem variações e o
 * regex captura o caso de uso real (links explícitos em jobs).
 */
@Slf4j
@Component
public class KettleDependencyParser {

  /** Captura conteúdo de <filename>...</filename>. */
  private static final Pattern FILENAME_TAG =
      Pattern.compile("<filename>\\s*([^<]+?)\\s*</filename>", Pattern.CASE_INSENSITIVE);

  /** Extensões de interesse (case-insensitive). */
  private static final Set<String> EXTENSOES = Set.of(".ktr", ".kjb");

  /**
   * Lê o conteúdo do arquivo Kettle e devolve um conjunto ordenado (insertion)
   * de paths literais para .ktr/.kjb encontrados. Caller é responsável por
   * fechar o stream.
   *
   * @param entrada stream UTF-8 do .ktr ou .kjb
   * @return paths como aparecem no XML (sem normalização adicional)
   */
  public Set<String> extrair(InputStream entrada) throws IOException {
    String conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
    Set<String> achados = new LinkedHashSet<>();

    Matcher m = FILENAME_TAG.matcher(conteudo);
    while (m.find()) {
      String raw = m.group(1).trim();
      if (raw.isEmpty()) continue;
      if (!ehLiteral(raw)) continue;
      String lower = raw.toLowerCase();
      if (EXTENSOES.stream().noneMatch(lower::endsWith)) continue;
      achados.add(raw);
    }
    if (!achados.isEmpty()) {
      log.debug("Kettle deps encontradas: {}", achados);
    }
    return achados;
  }

  /**
   * Path é literal (sem variável Kettle) e relativo (não começa com '/')?
   * Variáveis Kettle: ${NOME} ou %%NOME%%.
   */
  private boolean ehLiteral(String path) {
    if (path.startsWith("/")) return false;
    if (path.contains("${")) return false;
    if (path.contains("%%")) return false;
    return true;
  }

  /**
   * Resolve um path referenciado relativo ao diretório do arquivo que o
   * contém. Devolve o path normalizado relativo à raiz do repo.
   *
   * <p>Regras:
   * <ul>
   *   <li>{@code ../subjobs/x.ktr} relativo a {@code kettle/etl/main.kjb}
   *       vira {@code kettle/subjobs/x.ktr}.</li>
   *   <li>{@code subjobs/x.ktr} relativo a {@code kettle/etl/main.kjb}
   *       vira {@code kettle/etl/subjobs/x.ktr}.</li>
   * </ul>
   *
   * @param refPath path como veio do XML (relativo)
   * @param arquivoPai path do arquivo que referencia, relativo à raiz do repo
   * @return path normalizado relativo à raiz do repo, ou null se não resolveu
   */
  public String resolver(String refPath, String arquivoPai) {
    if (refPath == null || refPath.isBlank()) return null;
    int barra = arquivoPai.lastIndexOf('/');
    String diretorioPai = barra >= 0 ? arquivoPai.substring(0, barra) : "";

    String combinado = diretorioPai.isEmpty() ? refPath : diretorioPai + "/" + refPath;
    return normalizar(combinado);
  }

  /** Reduz {@code a/./b} e {@code a/x/../b} aplicando os segmentos. */
  private String normalizar(String path) {
    String[] partes = path.split("/");
    java.util.Deque<String> pilha = new java.util.ArrayDeque<>();
    for (String parte : partes) {
      if (parte.isEmpty() || parte.equals(".")) continue;
      if (parte.equals("..")) {
        if (!pilha.isEmpty() && !pilha.peekLast().equals("..")) {
          pilha.pollLast();
        } else {
          pilha.offerLast("..");
        }
      } else {
        pilha.offerLast(parte);
      }
    }
    return String.join("/", pilha);
  }
}

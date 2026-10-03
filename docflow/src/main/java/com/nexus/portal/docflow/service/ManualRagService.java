package com.nexus.portal.docflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.service.PaginaMarkdownConverter.Opcoes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Material para agentes e RAG a partir das páginas publicadas (Onda A, INT-101/102 + base RAG):
 *
 * <pre>
 * llms.txt                     índice do manual (padrão llmstxt.org): título, código e resumo de cada tela
 * llms-full.txt                todo o texto em Markdown, uma seção por tela
 * rag/&lt;projeto&gt;/&lt;CODIGO&gt;.md    uma tela por arquivo, com frontmatter YAML (metadados para filtro e citação)
 * rag/index.json               lista dos arquivos com sha256 — reindexe só o que mudou
 * </pre>
 *
 * Usado no pacote da publicação (snapshot por cliente e versão) e na exportação por projeto.
 */
@Service
public class ManualRagService {

  public static final String PASTA = "rag";
  public static final int VERSAO_FORMATO = 1;

  private final ObjectMapper json;

  public ManualRagService(ObjectMapper objectMapper) {
    this.json = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * @param titulo título do manual (ex.: "Manual Acme")
   * @param versao versão da publicação; nula na exportação por projeto
   * @param cliente slug do cliente; nulo na exportação por projeto
   * @param url tela → endereço da página no pacote ({@code paginas/x.html}); nulo = sem HTML
   * @param conteudoHtml tela → HTML final (trechos resolvidos, anexos reescritos)
   * @param incluirImagens {@code true} quando as imagens viajam junto (pacote); senão só a descrição
   */
  public record Escopo(
      String titulo,
      String versao,
      String cliente,
      Function<Pagina, String> url,
      Function<Pagina, String> conteudoHtml,
      boolean incluirImagens) {}

  /** Documento de uma tela, já convertido. */
  record Documento(Pagina pagina, String arquivo, String caminho, String markdown) {}

  public void escrever(Path raiz, List<Pagina> paginas, Escopo escopo) throws IOException {
    Map<String, String> arquivos = new LinkedHashMap<>();
    paginas.forEach(p -> arquivos.put(p.getCodigoTela(), arquivo(p)));
    List<Documento> documentos = paginas.stream()
        .map(p -> new Documento(p, arquivos.get(p.getCodigoTela()), caminho(p), corpo(p, escopo, arquivos)))
        .toList();

    String geradoEm = OffsetDateTime.now().toString();
    List<Map<String, Object>> indice = new ArrayList<>();
    for (Documento doc : documentos) {
      String conteudo = frontmatter(doc, escopo) + "# " + doc.pagina().getTitulo() + "\n\n"
          + resumo(doc.pagina()) + doc.markdown() + "\n";
      Path destino = raiz.resolve(PASTA).resolve(doc.arquivo());
      Files.createDirectories(destino.getParent());
      Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
      indice.add(itemIndice(doc, escopo, sha256(conteudo)));
    }

    Map<String, Object> index = new LinkedHashMap<>();
    index.put("formato", VERSAO_FORMATO);
    index.put("manual", escopo.titulo());
    index.put("versao", escopo.versao());
    index.put("cliente", escopo.cliente());
    index.put("geradoEm", geradoEm);
    index.put("documentos", indice);
    Files.createDirectories(raiz.resolve(PASTA));
    json.writeValue(raiz.resolve(PASTA).resolve("index.json").toFile(), index);
    Files.writeString(raiz.resolve(PASTA).resolve("README.md"), leiaMe(escopo), StandardCharsets.UTF_8);
    Files.writeString(raiz.resolve("llms.txt"), llms(documentos, escopo), StandardCharsets.UTF_8);
    Files.writeString(raiz.resolve("llms-full.txt"), llmsCompleto(documentos, escopo), StandardCharsets.UTF_8);
  }

  /** {@code <projeto>/<CODIGO>.md}: estável entre versões, então o RAG atualiza em vez de duplicar. */
  static String arquivo(Pagina pagina) {
    return seguro(pagina.getModulo().getProjeto().getSlug()) + "/" + seguro(pagina.getCodigoTela()) + ".md";
  }

  private String corpo(Pagina pagina, Escopo escopo, Map<String, String> arquivos) {
    String proprio = arquivos.get(pagina.getCodigoTela());
    Function<String, String> linkTela = codigo -> {
      String alvo = arquivos.get(codigo);
      return alvo == null ? null : relativo(proprio, alvo);
    };
    Opcoes opcoes = escopo.incluirImagens()
        // As imagens do pacote ficam em assets/, dois níveis acima de rag/<projeto>/.
        ? new Opcoes(linkTela, (src, alt) -> "![" + alt + "](" + src.replaceFirst("^\\.\\./assets/", "../../assets/") + ")")
        : new Opcoes(linkTela, Opcoes.texto().imagem());
    return PaginaMarkdownConverter.converter(escopo.conteudoHtml().apply(pagina), opcoes);
  }

  /** Sem data de geração: o sha256 do arquivo só muda quando a tela muda. */
  private String frontmatter(Documento doc, Escopo escopo) {
    Pagina p = doc.pagina();
    Map<String, Object> campos = new LinkedHashMap<>();
    campos.put("codigoTela", p.getCodigoTela());
    campos.put("titulo", p.getTitulo());
    campos.put("resumo", p.getResumo());
    campos.put("projeto", p.getModulo().getProjeto().getNome());
    campos.put("modulo", p.getModulo().getNome());
    campos.put("caminho", doc.caminho());
    campos.put("pai", p.getParent() == null ? null : p.getParent().getCodigoTela());
    campos.put("url", escopo.url() == null ? null : escopo.url().apply(p));
    campos.put("manual", escopo.titulo());
    campos.put("versao", escopo.versao());
    campos.put("cliente", escopo.cliente());
    campos.put("publicadoEm", p.getPublishedAt() == null ? null : p.getPublishedAt().toString());
    StringBuilder yaml = new StringBuilder("---\n");
    campos.forEach((chave, valor) -> {
      if (valor != null) {
        yaml.append(chave).append(": ").append(escalar(valor.toString())).append('\n');
      }
    });
    return yaml.append("---\n\n").toString();
  }

  private Map<String, Object> itemIndice(Documento doc, Escopo escopo, String sha256) {
    Pagina p = doc.pagina();
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("codigoTela", p.getCodigoTela());
    item.put("titulo", p.getTitulo());
    item.put("projeto", p.getModulo().getProjeto().getNome());
    item.put("modulo", p.getModulo().getNome());
    item.put("caminho", doc.caminho());
    item.put("arquivo", PASTA + "/" + doc.arquivo());
    item.put("url", escopo.url() == null ? null : escopo.url().apply(p));
    item.put("sha256", sha256);
    item.put("publicadoEm", p.getPublishedAt() == null ? null : p.getPublishedAt().toString());
    return item;
  }

  /** INT-101: índice no formato llms.txt, agrupado por projeto / módulo. */
  private String llms(List<Documento> documentos, Escopo escopo) {
    StringBuilder out = new StringBuilder("# ").append(tituloComVersao(escopo)).append("\n\n")
        .append("> Manual de uso gerado pelo Nexus DocFlow. Cada tela tem um código (ex.: ")
        .append(documentos.isEmpty() ? "PED-001" : documentos.getFirst().pagina().getCodigoTela())
        .append(") que identifica a página. O texto completo está em llms-full.txt e há um Markdown por tela em ")
        .append(PASTA).append("/ (índice em ").append(PASTA).append("/index.json).\n");
    Map<String, List<Documento>> grupos = documentos.stream().collect(Collectors.groupingBy(
        d -> d.pagina().getModulo().getProjeto().getNome() + " / " + d.pagina().getModulo().getNome(),
        LinkedHashMap::new, Collectors.toList()));
    grupos.forEach((grupo, docs) -> {
      out.append("\n## ").append(grupo).append("\n\n");
      for (Documento d : docs) {
        Pagina p = d.pagina();
        String destino = escopo.url() == null ? PASTA + "/" + d.arquivo() : escopo.url().apply(p);
        out.append("- [").append(p.getTitulo()).append("](").append(destino).append("): ")
            .append(p.getCodigoTela());
        if (p.getResumo() != null && !p.getResumo().isBlank()) {
          out.append(" — ").append(p.getResumo().strip().replaceAll("\\s+", " "));
        }
        out.append('\n');
      }
    });
    return out.toString();
  }

  /** INT-102: todo o conteúdo, uma seção por tela. */
  private String llmsCompleto(List<Documento> documentos, Escopo escopo) {
    StringBuilder out = new StringBuilder("# ").append(tituloComVersao(escopo)).append("\n");
    for (Documento d : documentos) {
      Pagina p = d.pagina();
      out.append("\n---\n\n# ").append(p.getTitulo()).append("\n\n")
          .append("Código da tela: ").append(p.getCodigoTela()).append(" · ").append(d.caminho()).append("\n\n")
          .append(resumo(p)).append(d.markdown()).append('\n');
    }
    return out.toString();
  }

  private static String leiaMe(Escopo escopo) {
    return """
        # %s — base para RAG

        Um arquivo Markdown por tela, em `<projeto>/<CODIGO_DA_TELA>.md`. O nome do arquivo não muda
        entre versões: reindexe pelo `sha256` de `index.json` para atualizar só o que mudou.

        O frontmatter YAML de cada arquivo traz `codigoTela`, `titulo`, `projeto`, `modulo`, `caminho`
        (posição no manual), `pai`, `url` (página no pacote, quando houver), `versao` e `publicadoEm`.
        Use-os como metadados do chunk para filtrar por projeto/módulo e para citar a tela na resposta.

        Os títulos começam em `##` dentro de cada tela: cortar os chunks por `##` mantém cada seção
        inteira. Links entre telas apontam para o `.md` da outra tela.
        """.formatted(tituloComVersao(escopo));
  }

  private static String tituloComVersao(Escopo escopo) {
    return escopo.titulo() + (escopo.versao() == null ? "" : " (v" + escopo.versao() + ")");
  }

  private static String resumo(Pagina pagina) {
    String resumo = pagina.getResumo();
    return resumo == null || resumo.isBlank() ? "" : "> " + resumo.strip().replaceAll("\\s+", " ") + "\n\n";
  }

  private static String caminho(Pagina pagina) {
    List<String> partes = new ArrayList<>();
    partes.add(pagina.getModulo().getProjeto().getNome());
    partes.add(pagina.getModulo().getNome());
    List<String> titulos = new ArrayList<>();
    for (Pagina atual = pagina; atual != null; atual = atual.getParent()) {
      titulos.addFirst(atual.getTitulo());
    }
    partes.addAll(titulos);
    return String.join(" › ", partes);
  }

  /** Caminho relativo entre dois arquivos de {@code rag/} ({@code proj/A.md} → {@code ../outro/B.md}). */
  static String relativo(String de, String para) {
    String pastaDe = de.substring(0, de.indexOf('/'));
    String pastaPara = para.substring(0, para.indexOf('/'));
    return pastaDe.equals(pastaPara) ? para.substring(para.indexOf('/') + 1) : "../" + para;
  }

  private static String seguro(String valor) {
    String limpo = valor == null ? "" : valor.strip().replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
    return limpo.isEmpty() ? "sem-codigo" : limpo;
  }

  /** String YAML entre aspas (JSON é YAML válido): sobrevive a dois-pontos, aspas e acentos. */
  private String escalar(String valor) {
    try {
      return json.copy().disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(valor);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static String sha256(String conteudo) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}

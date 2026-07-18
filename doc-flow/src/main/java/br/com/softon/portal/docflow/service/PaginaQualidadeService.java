package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.Pagina;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class PaginaQualidadeService {

  private static final Pattern PLACEHOLDER = Pattern.compile(
      "\\b(explique|descreva|informe|liste|registre aqui|nome do campo|escreva uma resposta)\\b",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

  public ResultadoQualidade avaliar(Pagina pagina) {
    String html = pagina.getConteudoHtml() == null ? "" : pagina.getConteudoHtml();
    Document document = Jsoup.parseBodyFragment(html);
    String texto = document.text().trim();
    List<ItemQualidade> itens = new ArrayList<>();

    itens.add(item("TITULO", "Título definido", "Informe um título claro para a página.",
        pagina.getTitulo() != null && !pagina.getTitulo().isBlank(), Severidade.ERRO));
    itens.add(item("CODIGO_TELA", "Código da tela definido", "Vincule a documentação à tela correta.",
        pagina.getCodigoTela() != null && !pagina.getCodigoTela().isBlank(), Severidade.ERRO));
    itens.add(item("CONTEUDO", "Conteúdo desenvolvido",
        "A página precisa ter pelo menos 80 caracteres de conteúdo útil.", texto.length() >= 80,
        Severidade.ERRO));
    itens.add(item("PLACEHOLDERS", "Textos de orientação substituídos",
        "Remova instruções do modelo como “Explique”, “Descreva” ou “Nome do campo”.",
        !PLACEHOLDER.matcher(texto.toLowerCase(Locale.ROOT)).find(), Severidade.ERRO));
    itens.add(item("IMAGENS_ALT", "Imagens acessíveis", "Toda imagem deve possuir texto alternativo.",
        document.select("img").stream().allMatch(img -> !img.attr("alt").isBlank()), Severidade.ERRO));
    itens.add(item("RESUMO", "Resumo preenchido", "Inclua uma descrição curta para buscas e navegação.",
        pagina.getResumo() != null && pagina.getResumo().trim().length() >= 30, Severidade.AVISO));
    itens.add(item("SECOES", "Conteúdo organizado em seções",
        "Use ao menos um título de seção para facilitar a leitura.", !document.select("h2, h3").isEmpty(),
        Severidade.AVISO));

    boolean apto = itens.stream().noneMatch(item -> item.severidade() == Severidade.ERRO && !item.ok());
    return new ResultadoQualidade(apto, List.copyOf(itens));
  }

  private ItemQualidade item(String codigo, String titulo, String descricao, boolean ok,
      Severidade severidade) {
    return new ItemQualidade(codigo, titulo, descricao, ok, severidade);
  }

  public enum Severidade {
    ERRO,
    AVISO
  }

  public record ItemQualidade(
      String codigo,
      String titulo,
      String descricao,
      boolean ok,
      Severidade severidade) {
  }

  public record ResultadoQualidade(boolean aptoParaRevisao, List<ItemQualidade> itens) {
  }
}

package com.nexus.portal.ai.service;

import com.nexus.portal.ai.dto.request.AiTemplateRecomendacaoRequest;
import com.nexus.portal.ai.dto.response.AiTemplateCandidatoResponse;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.ai.integration.docflow.AiTemplateSelector;
import com.nexus.portal.ai.service.AiDocumentoExtratorService.DocumentoExtraido;
import com.nexus.portal.shared.exception.BusinessException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AiDocumentoPlanejadorService {

  private static final int MAXIMO_PAGINAS = 80;
  private static final int MAXIMO_BRIEFING_PAGINA = 45_000;
  private static final Pattern TITULO_MARKDOWN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
  private static final Pattern TITULO_NUMERADO =
      Pattern.compile("^(\\d+(?:\\.\\d+){0,3})[.)]?\\s+(.{3,100})$");

  private final AiTemplateRecomendacaoService templateService;

  public AiDocumentoPlanejadorService(AiTemplateRecomendacaoService templateService) {
    this.templateService = templateService;
  }

  AiDocumentoPlano planejar(DocumentoExtraido documento, UUID projetoId, UUID clienteId) {
    String texto = inferirTitulos(documento.texto());
    String projetoNome = sugerirProjeto(documento.nomeArquivo(), texto);
    List<ModuloRascunho> rascunhos = montarHierarquia(texto, projetoNome);
    List<AiDocumentoPlano.Modulo> modulos = new ArrayList<>();
    int totalPaginas = 0;

    for (int indiceModulo = 0; indiceModulo < rascunhos.size(); indiceModulo++) {
      ModuloRascunho modulo = rascunhos.get(indiceModulo);
      List<AiDocumentoPlano.Pagina> paginas = new ArrayList<>();
      for (PaginaRascunho pagina : modulo.paginas()) {
        List<String> partes = dividirConteudo(pagina.conteudo());
        for (int parte = 0; parte < partes.size(); parte++) {
          totalPaginas++;
          if (totalPaginas > MAXIMO_PAGINAS) {
            throw new BusinessException(
                "O documento resultou em mais de 80 páginas. Divida o manual em arquivos menores.");
          }
          String titulo = partes.size() == 1
              ? pagina.titulo()
              : pagina.titulo() + " · parte " + (parte + 1);
          String briefing = briefing(projetoNome, modulo.nome(), titulo, partes.get(parte));
          String contextoTemplate = titulo + "\n\n" + partes.get(parte);
          paginas.add(criarPagina(
              titulo,
              paginas.size() + 1,
              briefing,
              contextoTemplate,
              projetoId,
              clienteId));
        }
      }
      if (!paginas.isEmpty()) {
        modulos.add(new AiDocumentoPlano.Modulo(
            UUID.randomUUID(), limitar(modulo.nome(), 180), indiceModulo + 1, List.copyOf(paginas)));
      }
    }

    if (modulos.isEmpty()) {
      throw new BusinessException("Não foi possível identificar conteúdo suficiente para criar o plano do manual.");
    }
    return new AiDocumentoPlano(limitar(projetoNome, 180), List.copyOf(modulos));
  }

  private AiDocumentoPlano.Pagina criarPagina(
      String titulo,
      int ordem,
      String briefing,
      String contextoTemplate,
      UUID projetoId,
      UUID clienteId) {
    AiTemplateCandidatoResponse template = recomendarTemplate(contextoTemplate, projetoId, clienteId);
    boolean selecaoAutomatica = template != null
        && template.confianca() >= AiTemplateSelector.CONFIANCA_AUTO_SELECAO;
    return new AiDocumentoPlano.Pagina(
        UUID.randomUUID(),
        limitar(titulo, 180),
        ordem,
        briefing,
        selecaoAutomatica ? template.templateId() : null,
        template == null ? null : template.codigo(),
        template == null ? null : template.nome(),
        template == null ? 0 : template.confianca(),
        template == null ? "Nenhum modelo apresentou correspondência suficiente." : template.motivo(),
        AiPaginaPlanoStatus.PENDENTE);
  }

  private AiTemplateCandidatoResponse recomendarTemplate(String briefing, UUID projetoId, UUID clienteId) {
    try {
      return templateService.recomendar(new AiTemplateRecomendacaoRequest(briefing, projetoId, clienteId)).recomendado();
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private List<ModuloRascunho> montarHierarquia(String texto, String projetoNome) {
    List<Linha> linhas = texto.lines().map(this::interpretarLinha).toList();
    boolean possuiTitulo = linhas.stream().anyMatch(linha -> linha.nivel() > 0);
    if (!possuiTitulo) return montarPorBlocos(texto);
    int maiorNivel = linhas.stream().mapToInt(Linha::nivel).max().orElse(0);
    int nivelPagina = maiorNivel >= 3 ? 3 : maiorNivel >= 2 ? 2 : 1;
    int nivelModulo = nivelPagina - 1;

    var modulos = new ArrayList<ModuloRascunhoMutavel>();
    ModuloRascunhoMutavel modulo = nivelModulo == 0
        ? new ModuloRascunhoMutavel("Conteúdo importado")
        : null;
    if (modulo != null) modulos.add(modulo);
    PaginaRascunhoMutavel pagina = null;
    var introducao = new StringBuilder();

    for (Linha linha : linhas) {
      if (nivelModulo > 0 && linha.nivel() == nivelModulo) {
        if (pagina != null && modulo != null) modulo.adicionar(pagina.finalizar());
        pagina = null;
        adicionarIntroducao(modulo, introducao);
        modulo = new ModuloRascunhoMutavel(linha.texto());
        modulos.add(modulo);
        continue;
      }
      if (linha.nivel() == nivelPagina) {
        if (modulo == null) {
          modulo = new ModuloRascunhoMutavel(projetoNome);
          modulos.add(modulo);
        }
        if (pagina != null) modulo.adicionar(pagina.finalizar());
        pagina = null;
        adicionarIntroducao(modulo, introducao);
        pagina = new PaginaRascunhoMutavel(linha.texto());
        continue;
      }
      if (linha.nivel() > nivelPagina) {
        String titulo = "#".repeat(Math.min(linha.nivel(), 6)) + " " + linha.texto();
        if (pagina != null) pagina.adicionar(titulo);
        else introducao.append(titulo).append('\n');
        continue;
      }
      if (linha.nivel() > 0) {
        // Cabeçalho acima da hierarquia escolhida (normalmente o título do projeto).
        continue;
      }
      if (pagina != null) pagina.adicionar(linha.texto());
      else introducao.append(linha.texto()).append('\n');
    }
    if (pagina != null && modulo != null) modulo.adicionar(pagina.finalizar());
    adicionarIntroducao(modulo, introducao);

    return modulos.stream()
        .filter(item -> !item.paginas.isEmpty())
        .map(ModuloRascunhoMutavel::finalizar)
        .toList();
  }

  private void adicionarIntroducao(ModuloRascunhoMutavel modulo, StringBuilder introducao) {
    String conteudo = introducao.toString().trim();
    introducao.setLength(0);
    if (modulo == null || conteudo.replaceAll("\\s", "").length() < 20) return;
    modulo.adicionar(new PaginaRascunho("Visão geral", conteudo));
  }

  private List<ModuloRascunho> montarPorBlocos(String texto) {
    List<String> blocos = dividirConteudo(texto);
    List<PaginaRascunho> paginas = new ArrayList<>();
    for (int i = 0; i < blocos.size(); i++) {
      String primeiraLinha = blocos.get(i).lines().findFirst().orElse("").trim();
      String titulo = primeiraLinha.length() >= 8 && primeiraLinha.length() <= 90
          ? primeiraLinha
          : "Conteúdo importado " + (i + 1);
      paginas.add(new PaginaRascunho(titulo, blocos.get(i)));
    }
    return List.of(new ModuloRascunho("Conteúdo importado", paginas));
  }

  private List<String> dividirConteudo(String conteudo) {
    String texto = conteudo == null ? "" : conteudo.trim();
    if (texto.length() <= MAXIMO_BRIEFING_PAGINA) return List.of(texto);
    List<String> partes = new ArrayList<>();
    var atual = new StringBuilder();
    for (String paragrafo : texto.split("\\n\\s*\\n")) {
      if (atual.length() > 0 && atual.length() + paragrafo.length() + 2 > MAXIMO_BRIEFING_PAGINA) {
        partes.add(atual.toString().trim());
        atual.setLength(0);
      }
      if (paragrafo.length() > MAXIMO_BRIEFING_PAGINA) {
        int inicio = 0;
        while (inicio < paragrafo.length()) {
          int fim = Math.min(inicio + MAXIMO_BRIEFING_PAGINA, paragrafo.length());
          if (!atual.isEmpty()) {
            partes.add(atual.toString().trim());
            atual.setLength(0);
          }
          partes.add(paragrafo.substring(inicio, fim).trim());
          inicio = fim;
        }
      } else {
        if (!atual.isEmpty()) atual.append("\n\n");
        atual.append(paragrafo);
      }
    }
    if (!atual.isEmpty()) partes.add(atual.toString().trim());
    return partes.stream().filter(parte -> !parte.isBlank()).toList();
  }

  private String briefing(String projeto, String modulo, String pagina, String conteudo) {
    return ("# Projeto: " + projeto + "\n\n"
        + "## Módulo: " + modulo + "\n\n"
        + "### Página: " + pagina + "\n\n"
        + conteudo).trim();
  }

  private String sugerirProjeto(String nomeArquivo, String texto) {
    Matcher primeiroTitulo = TITULO_MARKDOWN.matcher(texto.lines().findFirst().orElse(""));
    if (primeiroTitulo.matches() && primeiroTitulo.group(1).length() == 1) {
      return primeiroTitulo.group(2).trim();
    }
    String semExtensao = nomeArquivo.replaceFirst("(?i)\\.(doc|docx|pdf|txt)$", "");
    String legivel = semExtensao.replaceAll("[_-]+", " ").replaceAll("\\s+", " ").trim();
    return legivel.isBlank() ? "Manual importado" : legivel;
  }

  private String inferirTitulos(String texto) {
    if (texto.lines().anyMatch(linha -> TITULO_MARKDOWN.matcher(linha.trim()).matches())) return texto;
    List<String> linhas = texto.lines().toList();
    long titulosNumerados = linhas.stream()
        .map(String::trim)
        .filter(linha -> TITULO_NUMERADO.matcher(linha).matches())
        .count();
    boolean inferirTitulosEmMaiusculas = titulosNumerados < 2;
    var resultado = new StringBuilder();
    for (String linha : linhas) {
      String limpa = linha.trim();
      Matcher numerado = TITULO_NUMERADO.matcher(limpa);
      if (numerado.matches()) {
        int nivel = Math.min(4, numerado.group(1).split("\\.").length);
        resultado.append("#".repeat(nivel)).append(' ').append(numerado.group(2)).append('\n');
      } else if (inferirTitulosEmMaiusculas && pareceTitulo(limpa)) {
        resultado.append("## ").append(limpa).append('\n');
      } else {
        resultado.append(linha).append('\n');
      }
    }
    return resultado.toString().trim();
  }

  private boolean pareceTitulo(String linha) {
    if (linha.length() < 5 || linha.length() > 80 || linha.matches(".*[.;,:!?]$")) return false;
    String letras = linha.replaceAll("[^\\p{L}]", "");
    return letras.length() >= 5 && linha.equals(linha.toUpperCase(Locale.forLanguageTag("pt-BR")));
  }

  private Linha interpretarLinha(String original) {
    String linha = original.trim();
    Matcher titulo = TITULO_MARKDOWN.matcher(linha);
    if (titulo.matches()) return new Linha(titulo.group(1).length(), titulo.group(2).trim());
    return new Linha(0, original);
  }

  private String limitar(String valor, int maximo) {
    String limpo = Normalizer.normalize(valor == null ? "" : valor.trim(), Normalizer.Form.NFC);
    return limpo.length() <= maximo ? limpo : limpo.substring(0, maximo).trim();
  }

  private record Linha(int nivel, String texto) {}

  private record ModuloRascunho(String nome, List<PaginaRascunho> paginas) {}

  private record PaginaRascunho(String titulo, String conteudo) {}

  private static final class ModuloRascunhoMutavel {
    private final String nome;
    private final List<PaginaRascunho> paginas = new ArrayList<>();

    private ModuloRascunhoMutavel(String nome) {
      this.nome = nome;
    }

    private void adicionar(PaginaRascunho pagina) {
      if (pagina.conteudo() != null && !pagina.conteudo().isBlank()) paginas.add(pagina);
    }

    private ModuloRascunho finalizar() {
      return new ModuloRascunho(nome, List.copyOf(paginas));
    }
  }

  private static final class PaginaRascunhoMutavel {
    private final String titulo;
    private final StringBuilder conteudo = new StringBuilder();

    private PaginaRascunhoMutavel(String titulo) {
      this.titulo = titulo;
    }

    private void adicionar(String linha) {
      conteudo.append(linha).append('\n');
    }

    private PaginaRascunho finalizar() {
      return new PaginaRascunho(titulo, conteudo.toString().trim());
    }
  }
}

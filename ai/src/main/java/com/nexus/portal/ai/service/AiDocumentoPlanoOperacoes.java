package com.nexus.portal.ai.service;

import com.nexus.portal.ai.entity.AiPaginaPlanoOrigem;
import com.nexus.portal.ai.entity.AiPaginaPlanoStatus;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

/**
 * Operações puras sobre o {@link AiDocumentoPlano} de uma importação: aplicar sugestões da
 * análise (adicionar, renomear, mover, mesclar), localizar e copiar páginas, normalizar ordens.
 *
 * <p>O plano é imutável: toda operação devolve uma nova lista de módulos. Persistência, auditoria
 * e regras de fluxo ficam em {@link AiDocumentoImportacaoService}.
 */
final class AiDocumentoPlanoOperacoes {

  private AiDocumentoPlanoOperacoes() {}

  private record PaginaLocalizada(AiDocumentoPlano.Modulo modulo, AiDocumentoPlano.Pagina pagina) {}

  static List<AiDocumentoPlano.Modulo> aplicarSugestao(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    return switch (sugestao.tipo()) {
      case ADICIONAR_PAGINA -> adicionarPagina(plano, sugestao);
      case RENOMEAR_PAGINA -> renomearPagina(plano, sugestao);
      case MOVER_PAGINA -> moverPagina(plano, sugestao);
      case MESCLAR_PAGINAS -> mesclarPaginas(plano, sugestao);
      case RENOMEAR_MODULO -> renomearModulo(plano, sugestao);
    };
  }

  private static List<AiDocumentoPlano.Modulo> adicionarPagina(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    long totalPaginas = plano.modulos().stream().mapToLong(item -> item.paginas().size()).sum();
    if (totalPaginas >= AiDocumentoPlanejadorService.MAXIMO_PAGINAS) {
      throw new BusinessException(
          "O plano já atingiu o limite de "
              + AiDocumentoPlanejadorService.MAXIMO_PAGINAS + " páginas.");
    }
    UUID moduloId = sugestao.moduloDestinoId() != null
        ? sugestao.moduloDestinoId()
        : sugestao.moduloOrigemId();
    AiDocumentoPlano.Modulo destino = encontrarModulo(plano.modulos(), moduloId);
    String titulo = textoObrigatorio(sugestao.valorSugerido(), "Informe o título da página sugerida.");
    String conteudo = sugestao.conteudoSugerido() == null || sugestao.conteudoSugerido().isBlank()
        ? "> Página proposta pela análise de completude. Revise e complemente antes de gerar.\n\n"
            + sugestao.justificativa()
        : sugestao.conteudoSugerido().trim();
    AiDocumentoPlano.Pagina pagina = new AiDocumentoPlano.Pagina(
        UUID.randomUUID(),
        titulo,
        destino.paginas().size() + 1,
        montarBriefing(plano.projetoNome(), destino.nome(), titulo, conteudo),
        null,
        null,
        null,
        0,
        "Página adicionada pela análise de completude; o modelo será escolhido na geração.",
        AiPaginaPlanoStatus.PENDENTE,
        null,
        null,
        null,
        AiPaginaPlanoOrigem.IA,
        false);
    return plano.modulos().stream()
        .map(modulo -> modulo.id().equals(destino.id())
            ? new AiDocumentoPlano.Modulo(
                modulo.id(),
                modulo.moduloId(),
                modulo.nome(),
                modulo.ordem(),
                append(modulo.paginas(), pagina))
            : modulo)
        .toList();
  }

  private static List<AiDocumentoPlano.Modulo> renomearPagina(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    String titulo = textoObrigatorio(sugestao.valorSugerido(), "Informe o novo título da página.");
    localizarPagina(plano.modulos(), sugestao.paginaOrigemId());
    return plano.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> pagina.id().equals(sugestao.paginaOrigemId())
                    ? copiarPagina(
                        pagina,
                        titulo,
                        atualizarCabecalhoPagina(pagina.briefing(), titulo),
                        pagina.ordem())
                    : pagina)
                .toList()))
        .toList();
  }

  private static List<AiDocumentoPlano.Modulo> renomearModulo(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    UUID moduloId = sugestao.moduloOrigemId() != null
        ? sugestao.moduloOrigemId()
        : sugestao.moduloDestinoId();
    encontrarModulo(plano.modulos(), moduloId);
    String nome = textoObrigatorio(sugestao.valorSugerido(), "Informe o novo nome do módulo.");
    if (plano.modulos().stream()
        .anyMatch(item -> !item.id().equals(moduloId) && item.nome().equalsIgnoreCase(nome))) {
      throw new BusinessException("Já existe outro módulo com o nome sugerido.");
    }
    return plano.modulos().stream()
        .map(modulo -> modulo.id().equals(moduloId)
            ? new AiDocumentoPlano.Modulo(
                modulo.id(),
                modulo.moduloId(),
                nome,
                modulo.ordem(),
                modulo.paginas().stream()
                    .map(pagina -> atualizarContexto(pagina, plano.projetoNome(), nome))
                    .toList())
            : modulo)
        .toList();
  }

  private static List<AiDocumentoPlano.Modulo> moverPagina(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    PaginaLocalizada localizada = localizarPagina(plano.modulos(), sugestao.paginaOrigemId());
    AiDocumentoPlano.Modulo destino = encontrarModulo(plano.modulos(), sugestao.moduloDestinoId());
    if (localizada.modulo().id().equals(destino.id())) {
      throw new BusinessException("A página sugerida já pertence ao módulo de destino.");
    }
    AiDocumentoPlano.Pagina movida = atualizarContexto(
        localizada.pagina(), plano.projetoNome(), destino.nome());
    List<AiDocumentoPlano.Modulo> modulos = plano.modulos().stream()
        .map(modulo -> {
          List<AiDocumentoPlano.Pagina> paginas = new ArrayList<>(modulo.paginas());
          if (modulo.id().equals(localizada.modulo().id())) {
            paginas.removeIf(item -> item.id().equals(movida.id()));
          }
          if (modulo.id().equals(destino.id())) paginas.add(movida);
          return new AiDocumentoPlano.Modulo(
              modulo.id(), modulo.moduloId(), modulo.nome(), modulo.ordem(), List.copyOf(paginas));
        })
        .toList();
    return normalizarOrdens(modulos);
  }

  private static List<AiDocumentoPlano.Modulo> mesclarPaginas(
      AiDocumentoPlano plano, AiDocumentoPlano.Sugestao sugestao) {
    if (sugestao.paginaOrigemId().equals(sugestao.paginaDestinoId())) {
      throw new BusinessException("As páginas de origem e destino da mesclagem devem ser diferentes.");
    }
    PaginaLocalizada origem = localizarPagina(plano.modulos(), sugestao.paginaOrigemId());
    PaginaLocalizada destino = localizarPagina(plano.modulos(), sugestao.paginaDestinoId());
    String titulo = sugestao.valorSugerido() == null || sugestao.valorSugerido().isBlank()
        ? destino.pagina().titulo()
        : sugestao.valorSugerido().trim();
    String orientacao = sugestao.conteudoSugerido() == null || sugestao.conteudoSugerido().isBlank()
        ? ""
        : "> Orientação da análise: " + sugestao.conteudoSugerido().trim() + "\n\n";
    String conteudo = orientacao
        + removerCabecalho(destino.pagina().briefing())
        + "\n\n## Conteúdo consolidado de " + origem.pagina().titulo() + "\n\n"
        + removerCabecalho(origem.pagina().briefing());
    AiDocumentoPlano.Pagina mesclada = copiarPagina(
        destino.pagina(),
        titulo,
        montarBriefing(plano.projetoNome(), destino.modulo().nome(), titulo, conteudo),
        destino.pagina().ordem());
    List<AiDocumentoPlano.Modulo> modulos = plano.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .filter(pagina -> !pagina.id().equals(origem.pagina().id()))
                .map(pagina -> pagina.id().equals(destino.pagina().id()) ? mesclada : pagina)
                .toList()))
        .toList();
    return normalizarOrdens(modulos);
  }

  static AiDocumentoPlano atualizarPagina(
      AiDocumentoPlano atual,
      UUID paginaPlanoId,
      UnaryOperator<AiDocumentoPlano.Pagina> atualizador) {
    List<AiDocumentoPlano.Modulo> modulos = atual.modulos().stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            modulo.moduloId(),
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> pagina.id().equals(paginaPlanoId) ? atualizador.apply(pagina) : pagina)
                .toList()))
        .toList();
    return new AiDocumentoPlano(
        atual.projetoNome(),
        atual.projetoDescricao(),
        atual.projetoId(),
        atual.clienteId(),
        atual.estruturaConfirmada(),
        modulos,
        atual.projetoNomesSugeridos(),
        atual.analiseOrigem(),
        atual.analiseMensagem(),
        atual.tokensEntradaAnalise(),
        atual.tokensSaidaAnalise(),
        atual.sugestoes());
  }

  private static AiDocumentoPlano.Modulo encontrarModulo(
      List<AiDocumentoPlano.Modulo> modulos, UUID moduloId) {
    if (moduloId == null) throw new BusinessException("A sugestão não informa o módulo de destino.");
    return modulos.stream()
        .filter(item -> item.id().equals(moduloId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Módulo da sugestão não encontrado no plano."));
  }

  private static PaginaLocalizada localizarPagina(
      List<AiDocumentoPlano.Modulo> modulos, UUID paginaId) {
    if (paginaId == null) throw new BusinessException("A sugestão não informa a página de origem.");
    for (AiDocumentoPlano.Modulo modulo : modulos) {
      for (AiDocumentoPlano.Pagina pagina : modulo.paginas()) {
        if (pagina.id().equals(paginaId)) return new PaginaLocalizada(modulo, pagina);
      }
    }
    throw new NotFoundException("Página da sugestão não encontrada no plano.");
  }

  private static List<AiDocumentoPlano.Modulo> normalizarOrdens(
      List<AiDocumentoPlano.Modulo> modulos) {
    return modulos.stream()
        .map(modulo -> {
          List<AiDocumentoPlano.Pagina> paginas = new ArrayList<>();
          for (int indice = 0; indice < modulo.paginas().size(); indice++) {
            AiDocumentoPlano.Pagina pagina = modulo.paginas().get(indice);
            paginas.add(copiarPagina(pagina, pagina.titulo(), pagina.briefing(), indice + 1));
          }
          return new AiDocumentoPlano.Modulo(
              modulo.id(), modulo.moduloId(), modulo.nome(), modulo.ordem(), List.copyOf(paginas));
        })
        .toList();
  }

  static AiDocumentoPlano.Pagina copiarPagina(
      AiDocumentoPlano.Pagina pagina, String titulo, String briefing, int ordem) {
    return new AiDocumentoPlano.Pagina(
        pagina.id(),
        titulo,
        ordem,
        briefing,
        pagina.templateId(),
        pagina.templateCodigo(),
        pagina.templateNome(),
        pagina.confiancaTemplate(),
        pagina.motivoTemplate(),
        pagina.status(),
        pagina.paginaId(),
        pagina.sessaoId(),
        pagina.erroMensagem(),
        pagina.origem(),
        pagina.ajustadaManualmente(),
        pagina.blueprintId(),
        pagina.blueprintNome(),
        pagina.componentesSelecionados(),
        pagina.componentesObrigatorios(),
        pagina.composicaoAjustadaManualmente());
  }

  /** Reescreve o cabeçalho "# Projeto / ## Módulo" do briefing após renomear ou mover. */
  static AiDocumentoPlano.Pagina atualizarContexto(
      AiDocumentoPlano.Pagina pagina,
      String projetoNome,
      String moduloNome) {
    String briefing = pagina.briefing()
        .replaceFirst("(?m)^# Projeto:.*$", Matcher.quoteReplacement("# Projeto: " + projetoNome))
        .replaceFirst("(?m)^## Módulo:.*$", Matcher.quoteReplacement("## Módulo: " + moduloNome));
    return copiarPagina(pagina, pagina.titulo(), briefing, pagina.ordem());
  }

  private static String atualizarCabecalhoPagina(String briefing, String titulo) {
    return briefing.replaceFirst(
        "(?m)^### Página:.*$", Matcher.quoteReplacement("### Página: " + titulo));
  }

  static String montarBriefing(
      String projetoNome, String moduloNome, String paginaTitulo, String conteudo) {
    return "# Projeto: " + projetoNome + "\n\n"
        + "## Módulo: " + moduloNome + "\n\n"
        + "### Página: " + paginaTitulo + "\n\n"
        + conteudo.trim();
  }

  static String removerCabecalho(String briefing) {
    return briefing.replaceFirst(
        "(?s)^# Projeto:[^\\r\\n]*(?:\\R)+## Módulo:[^\\r\\n]*(?:\\R)+"
            + "### Página:[^\\r\\n]*(?:\\R)+",
        "").trim();
  }

  private static String textoObrigatorio(String valor, String mensagem) {
    if (valor == null || valor.isBlank()) throw new BusinessException(mensagem);
    return valor.trim();
  }

  private static <T> List<T> append(List<T> itens, T novoItem) {
    List<T> resultado = new ArrayList<>(itens);
    resultado.add(novoItem);
    return List.copyOf(resultado);
  }
}

package com.nexus.portal.ai.service;

import com.nexus.portal.ai.service.AiBriefingDocument.Secao;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Completa slots omitidos pelo LLM com conteúdo extraído do briefing.
 *
 * <p>O enriquecimento é conservador: nunca sobrescreve um texto fornecido pelo modelo e utiliza
 * somente slots existentes no catálogo canônico.
 */
@Component
public class AiBriefingPageSpecEnricher {

  public AiPageSpec enriquecer(
      AiPageSpec spec, List<PaginaBlocoResponse> candidatos, String briefing) {
    AiBriefingDocument documento = AiBriefingDocument.interpretar(briefing);
    Map<String, PaginaBlocoResponse> catalogo = candidatos.stream()
        .collect(java.util.stream.Collectors.toMap(
            PaginaBlocoResponse::id, bloco -> bloco, (primeiro, ignorado) -> primeiro));
    String tituloPagina = limparTitulo(spec.titulo(), documento.titulo());
    List<AiPageSpec.Bloco> blocos = new ArrayList<>();
    for (AiPageSpec.Bloco bloco : spec.blocos()) {
      PaginaBlocoResponse componente = catalogo.get(bloco.componenteId());
      if (componente == null) {
        blocos.add(bloco);
        continue;
      }
      Map<String, String> textos = new LinkedHashMap<>();
      bloco.textos().forEach(texto -> textos.putIfAbsent(texto.slotId(), texto.valor()));
      preencherComponente(componente, textos, documento, tituloPagina);
      List<AiPageSpec.Texto> enriquecidos = textos.entrySet().stream()
          .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
          .map(entry -> new AiPageSpec.Texto(entry.getKey(), entry.getValue()))
          .toList();
      if (!enriquecidos.isEmpty()) {
        blocos.add(new AiPageSpec.Bloco(bloco.componenteId(), enriquecidos));
      }
    }
    return new AiPageSpec(
        spec.schemaVersion(),
        spec.blueprintId(),
        tituloPagina,
        spec.slug(),
        spec.codigoTela(),
        resumo(spec, documento),
        List.copyOf(blocos));
  }

  private static void preencherComponente(
      PaginaBlocoResponse componente,
      Map<String, String> textos,
      AiBriefingDocument documento,
      String tituloPagina) {
    SlotWriter slots = new SlotWriter(componente, textos);
    switch (componente.id()) {
      case "introducao" -> preencherIntroducao(slots, documento, tituloPagina);
      case "objetivo" -> preencherObjetivo(slots, documento);
      case "pre-requisitos" -> preencherAcesso(slots, documento);
      case "visao-tela", "captura-anotada" -> preencherVisaoTela(slots, documento);
      case "filtros-resultado" -> preencherFiltros(slots, documento);
      case "acoes-tela" -> preencherAcoes(slots, documento);
      case "passo-a-passo" -> preencherPassos(slots, documento);
      case "campos-criticos" -> preencherCampos(slots, documento);
      case "dicionario" -> preencherDicionario(slots, documento);
      case "regras" -> preencherRegras(slots, documento);
      case "se-entao" -> preencherCondicao(slots, documento);
      case "permissoes" -> preencherPermissoes(slots, documento);
      case "mensagens-sistema" -> preencherMensagens(slots, documento);
      case "callout-erro" -> preencherCalloutErro(slots, documento);
      case "resultado-esperado" -> preencherResultado(slots, documento);
      case "boas-praticas" -> preencherRecomendacoes(slots, documento);
      case "checklist-validacao" -> preencherChecklist(slots, documento);
      default -> preencherPorAfinidade(slots, componente, documento);
    }
  }

  private static void preencherIntroducao(
      SlotWriter slots, AiBriefingDocument doc, String tituloPagina) {
    slots.put("t1", "Manual do usuário");
    slots.put("t2", tituloPagina);
    slots.put("t3", conteudo(doc, "historia do usuario", "visao geral", "objetivo"));
  }

  private static void preencherObjetivo(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Objetivo");
    slots.put("t2", conteudo(doc, "objetivo", "historia do usuario"));
  }

  private static void preencherAcesso(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Acesso à funcionalidade");
    distribuir(slots, List.of("t2", "t3", "t4"), trechos(
        doc, "acesso a funcionalidade", "controle de acesso", "pre requisitos"));
  }

  private static void preencherVisaoTela(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Visão da tela");
    slots.put("t4", primeiroTrecho(doc, "listagem de registros", "acesso a funcionalidade"));
  }

  private static void preencherFiltros(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Listagem, filtros e resultados");
    slots.put("t2", "Filtros e pesquisa");
    slots.put("t3", conteudo(doc, "listagem de registros"));
    slots.put("t4", "Ordenação, paginação e exportação");
    slots.put("t5", combinar(doc, "ordenacao e paginacao", "exportacao de dados"));
  }

  private static void preencherAcoes(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Operações disponíveis");
    slots.put("t5", "Pesquisar e limpar");
    slots.put("t6", conteudo(doc, "listagem de registros"));
    slots.put("t7", "Disponível conforme os critérios informados e o estado da consulta.");
    slots.put("t8", "Incluir, visualizar e editar");
    slots.put("t9", combinar(doc, "inclusao de um novo registro", "visualizacao dos detalhes", "edicao de um registro"));
    slots.put("t10", primeiroTrecho(doc, "controle de acesso", "acesso a funcionalidade"));
    slots.put("t11", "Excluir, ativar e inativar");
    slots.put("t12", combinar(doc, "exclusao de um registro", "ativacao e inativacao"));
    slots.put("t13", primeiroTrecho(doc, "exclusao de um registro", "controle de acesso"));
  }

  private static void preencherPassos(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Como utilizar a funcionalidade");
    distribuir(slots, List.of("t2", "t3", "t4", "t5"), List.of(
        primeiroTrecho(doc, "acesso a funcionalidade"),
        primeiroTrecho(doc, "inclusao de um novo registro", "listagem de registros"),
        primeiroTrecho(doc, "visualizacao dos detalhes", "edicao de um registro"),
        primeiroTrecho(doc, "exclusao de um registro", "ativacao e inativacao")));
  }

  private static void preencherCampos(SlotWriter slots, AiBriefingDocument doc) {
    String validacoes = conteudo(doc, "inclusao de um novo registro");
    slots.put("t1", "Campos e validações");
    slots.put("t5", "Campos obrigatórios");
    slots.put("t6", validacoes);
    slots.put("t7", "Sim");
    slots.put("t8", "Dados validados");
    slots.put("t9", validacoes);
    slots.put("t10", "Conforme a regra do campo");
  }

  private static void preencherDicionario(SlotWriter slots, AiBriefingDocument doc) {
    List<String> validacoes = doc.encontrar("inclusao de um novo registro")
        .map(Secao::itens)
        .orElse(List.of());
    slots.put("t1", "Campos e validações");
    slots.put("t9", validacoes.stream().findFirst().orElse("Campo obrigatório"));
    slots.put("t10", validacoes.size() > 1 ? validacoes.get(1) : conteudo(doc, "inclusao de um novo registro"));
    slots.put("t11", "Sim");
    slots.put("t15", validacoes.size() > 2 ? validacoes.get(2) : "Outro campo validado");
    slots.put("t16", validacoes.size() > 3 ? validacoes.get(3) : conteudo(doc, "edicao de um registro"));
    slots.put("t17", "Conforme a regra");
  }

  private static void preencherRegras(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Regras e restrições");
    slots.put("t2", "Validações");
    slots.put("t3", conteudo(doc, "inclusao de um novo registro"));
    slots.put("t4", "Exclusão e vínculos");
    slots.put("t5", conteudo(doc, "exclusao de um registro"));
    slots.put("t6", "Situação do registro");
    slots.put("t7", conteudo(doc, "ativacao e inativacao"));
  }

  private static void preencherCondicao(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Condições e efeitos");
    slots.put("t3", "Quando houver dados inválidos ou vínculos");
    slots.put("t4", combinar(doc, "inclusao de um novo registro", "exclusao de um registro"));
    slots.put("t6", "A operação é validada ou bloqueada");
    slots.put("t7", combinar(doc, "mensagens do sistema", "ativacao e inativacao"));
  }

  private static void preencherPermissoes(SlotWriter slots, AiBriefingDocument doc) {
    String permissoes = combinar(doc, "controle de acesso e auditoria", "acesso a funcionalidade");
    slots.put("t1", "Controle de acesso e auditoria");
    slots.put("t3", permissoes);
    slots.put("t5", permissoes);
    slots.put("t7", permissoes);
  }

  private static void preencherMensagens(SlotWriter slots, AiBriefingDocument doc) {
    List<String> mensagens = doc.encontrar("mensagens do sistema")
        .map(Secao::itens)
        .orElse(List.of());
    slots.put("t1", "Mensagens do sistema");
    if (!mensagens.isEmpty()) {
      int meio = Math.max(1, (mensagens.size() + 1) / 2);
      slots.put("t5", String.join(" • ", mensagens.subList(0, meio)));
      slots.put("t8", String.join(" • ", mensagens.subList(meio, mensagens.size())));
    }
    slots.put("t6", "Confirmação da operação ou validação dos dados informados.");
    slots.put("t7", "Confira a mensagem e siga a orientação apresentada.");
    slots.put("t9", "Registro não encontrado, duplicado ou relacionado a outros dados.");
    slots.put("t10", "Revise filtros, campos e vínculos; se persistir, acione o suporte.");
  }

  private static void preencherCalloutErro(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Mensagens e situações de erro");
    slots.put("t2", conteudo(doc, "mensagens do sistema"));
  }

  private static void preencherResultado(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Resultado esperado");
    List<String> mensagens = doc.encontrar("mensagens do sistema")
        .map(Secao::itens)
        .orElse(List.of());
    String sucessos = mensagens.stream()
        .filter(mensagem -> AiBriefingDocument.normalizar(mensagem).contains("sucesso"))
        .limit(3)
        .reduce((a, b) -> a + " " + b)
        .orElse("");
    slots.put("t2", sucessos.isBlank()
        ? combinar(doc, "edicao de um registro", "ativacao e inativacao")
        : sucessos);
  }

  private static void preencherRecomendacoes(SlotWriter slots, AiBriefingDocument doc) {
    List<String> recomendacoes = trechos(doc, "recomendacoes de uso", "boas praticas");
    slots.put("t1", "Recomendações de uso");
    distribuir(slots, List.of("t4", "t7", "t10"), recomendacoes);
  }

  private static void preencherChecklist(SlotWriter slots, AiBriefingDocument doc) {
    slots.put("t1", "Antes de concluir");
    slots.distribuirCorpo(trechos(doc, "recomendacoes de uso", "boas praticas"));
  }

  private static void preencherPorAfinidade(
      SlotWriter slots, PaginaBlocoResponse componente, AiBriefingDocument doc) {
    doc.melhorPara(componente.id() + " " + componente.nome() + " " + componente.descricao())
        .ifPresent(secao -> {
          slots.primeiroTitulo(secao.titulo());
          slots.distribuirCorpo(secao.trechos());
        });
  }

  private static String limparTitulo(String atual, String fallback) {
    String titulo = atual == null || atual.isBlank() ? fallback : atual.trim();
    return titulo.replaceFirst("^#{1,6}\\s+", "").trim();
  }

  private static String resumo(AiPageSpec spec, AiBriefingDocument documento) {
    String objetivo = conteudo(documento, "objetivo");
    if (!objetivo.isBlank()) {
      return objetivo;
    }
    String atual = spec.resumo() == null ? "" : spec.resumo().trim();
    String titulo = limparTitulo(spec.titulo(), documento.titulo());
    if (!atual.isBlank()
        && !AiBriefingDocument.normalizar(atual).equals(AiBriefingDocument.normalizar(titulo))) {
      return atual;
    }
    String historia = conteudo(documento, "historia do usuario");
    return historia.isBlank() ? atual : historia;
  }

  private static String conteudo(AiBriefingDocument doc, String... aliases) {
    return doc.encontrar(aliases).map(Secao::conteudo).orElse("");
  }

  private static String primeiroTrecho(AiBriefingDocument doc, String... aliases) {
    return doc.encontrar(aliases)
        .flatMap(secao -> secao.trechos().stream().findFirst())
        .orElse("");
  }

  private static List<String> trechos(AiBriefingDocument doc, String... aliases) {
    List<String> resultado = new ArrayList<>();
    for (String alias : aliases) {
      doc.encontrar(alias).ifPresent(secao -> secao.trechos().forEach(trecho -> {
        if (!resultado.contains(trecho)) {
          resultado.add(trecho);
        }
      }));
    }
    return List.copyOf(resultado);
  }

  private static String combinar(AiBriefingDocument doc, String... aliases) {
    List<String> conteudos = new ArrayList<>();
    for (String alias : aliases) {
      String conteudo = conteudo(doc, alias);
      if (!conteudo.isBlank() && !conteudos.contains(conteudo)) {
        conteudos.add(conteudo);
      }
    }
    return String.join(" ", conteudos);
  }

  private static void distribuir(SlotWriter slots, List<String> ids, List<String> valores) {
    int indice = 0;
    for (String valor : valores) {
      if (valor != null && !valor.isBlank() && indice < ids.size()) {
        slots.put(ids.get(indice++), valor);
      }
    }
  }

  private static final class SlotWriter {
    private final PaginaBlocoResponse componente;
    private final Map<String, String> textos;

    private SlotWriter(PaginaBlocoResponse componente, Map<String, String> textos) {
      this.componente = componente;
      this.textos = textos;
    }

    void put(String slotId, String valor) {
      if (valor == null || valor.isBlank() || textos.containsKey(slotId)) {
        return;
      }
      boolean existe = componente.slots().stream().anyMatch(slot -> slot.id().equals(slotId));
      if (existe) {
        textos.put(slotId, valor.trim());
      }
    }

    void primeiroTitulo(String titulo) {
      componente.slots().stream()
          .filter(slot -> slot.elemento().matches("h[1-6]"))
          .findFirst()
          .ifPresent(slot -> put(slot.id(), titulo));
    }

    void distribuirCorpo(List<String> valores) {
      List<String> slotsCorpo = componente.slots().stream()
          .filter(slot -> !slot.elemento().matches("h[1-6]|th"))
          .map(slot -> slot.id())
          .toList();
      distribuir(this, slotsCorpo, valores);
    }
  }
}

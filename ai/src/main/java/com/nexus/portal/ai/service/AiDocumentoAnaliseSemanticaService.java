package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoStatus;
import com.nexus.portal.ai.entity.AiDocumentoSugestaoTipo;
import com.nexus.portal.ai.provider.LlmCompletion;
import com.nexus.portal.ai.provider.LlmProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import org.springframework.stereotype.Service;

/** Reorganiza semanticamente o plano sem pedir à LLM que reproduza o conteúdo do documento. */
@Service
public class AiDocumentoAnaliseSemanticaService {

  private static final int MAXIMO_TRECHO_PAGINA = 650;
  private static final int MAXIMO_MODULOS = 30;
  private static final int MAXIMO_SUGESTOES = 12;

  private final LlmProvider llmProvider;
  private final ObjectMapper objectMapper;

  public AiDocumentoAnaliseSemanticaService(LlmProvider llmProvider, ObjectMapper objectMapper) {
    this.llmProvider = llmProvider;
    this.objectMapper = objectMapper;
  }

  boolean disponivel() {
    return !"fake".equals(llmProvider.id());
  }

  AiDocumentoPlano analisar(AiDocumentoPlano base, String nomeArquivo) {
    Map<UUID, AiDocumentoPlano.Pagina> paginasOriginais = new LinkedHashMap<>();
    List<Map<String, Object>> manifesto = new ArrayList<>();
    for (AiDocumentoPlano.Modulo modulo : base.modulos()) {
      for (AiDocumentoPlano.Pagina pagina : modulo.paginas()) {
        paginasOriginais.put(pagina.id(), pagina);
        manifesto.add(Map.of(
            "paginaId", pagina.id().toString(),
            "moduloAtual", modulo.nome(),
            "tituloAtual", pagina.titulo(),
            "trecho", trechoConteudo(pagina.briefing())));
      }
    }

    String userPrompt = """
        Arquivo: %s
        Nome estrutural atual: %s

        Organize o manifesto abaixo como um manual de usuário profissional.
        Cada paginaId deve aparecer exatamente uma vez. Não crie, remova, una ou divida páginas.
        Sugira até 3 nomes curtos para o projeto, uma descrição objetiva e módulos em ordem de uso.
        Renomeie páginas somente quando isso melhorar clareza e consistência.

        Depois analise a completude do manual e devolva no máximo 12 sugestões ainda não
        aplicadas na estrutura proposta. Use:
        - ADICIONAR_PAGINA apenas para uma lacuna funcional fortemente indicada pelo texto;
        - MESCLAR_PAGINAS apenas para páginas realmente duplicadas;
        - MOVER_PAGINA quando a jornada ficar materialmente mais clara;
        - RENOMEAR_PAGINA ou RENOMEAR_MODULO somente quando a estrutura proposta ainda precisar.
        O conteudoSugerido deve listar o que precisa ser documentado usando somente fatos
        sustentados pelo manifesto. Não invente campos, telas, permissões ou regras.

        MANIFESTO_JSON:
        %s
        """.formatted(nomeArquivo, base.projetoNome(), escrever(manifesto));
    String systemPrompt = """
        Você é arquiteto de informação especializado em manuais de software.
        Classifique páginas por jornada do usuário, pré-requisitos e dependências funcionais.
        Responda somente o JSON solicitado. Preserve todos os paginaId exatamente uma vez.
        """;

    LlmCompletion completion = llmProvider.completarEstruturado(systemPrompt, userPrompt, schema());
    JsonNode resposta = lerJson(completion.content());
    List<String> nomesSugeridos = lerNomes(resposta.path("projetoNomes"), base.projetoNome());
    String descricao = textoLimitado(
        resposta.path("projetoDescricao").asText(base.projetoDescricao()), 1_000, base.projetoDescricao());
    JsonNode modulosNode = resposta.path("modulos");
    if (!modulosNode.isArray() || modulosNode.isEmpty() || modulosNode.size() > MAXIMO_MODULOS) {
      throw new IllegalStateException("A IA retornou uma estrutura de módulos inválida.");
    }

    Set<UUID> utilizados = new HashSet<>();
    List<AiDocumentoPlano.Modulo> modulos = new ArrayList<>();
    int ordemModulo = 0;
    for (JsonNode moduloNode : modulosNode) {
      String nomeModulo = textoLimitado(moduloNode.path("nome").asText(), 150, "Conteúdo importado");
      JsonNode paginasNode = moduloNode.path("paginas");
      if (!paginasNode.isArray() || paginasNode.isEmpty()) continue;
      List<AiDocumentoPlano.Pagina> paginas = new ArrayList<>();
      int ordemPagina = 0;
      for (JsonNode paginaNode : paginasNode) {
        UUID paginaId = uuid(paginaNode.path("paginaId").asText());
        AiDocumentoPlano.Pagina original = paginasOriginais.get(paginaId);
        if (original == null || !utilizados.add(paginaId)) {
          throw new IllegalStateException("A IA alterou ou repetiu identificadores de páginas.");
        }
        String titulo = textoLimitado(paginaNode.path("titulo").asText(), 180, original.titulo());
        String briefing = atualizarCabecalho(original.briefing(), base.projetoNome(), nomeModulo, titulo);
        paginas.add(new AiDocumentoPlano.Pagina(
            original.id(),
            titulo,
            ++ordemPagina,
            briefing,
            original.templateId(),
            original.templateCodigo(),
            original.templateNome(),
            original.confiancaTemplate(),
            original.motivoTemplate(),
            original.status(),
            original.paginaId(),
            original.sessaoId(),
            original.erroMensagem()));
      }
      if (!paginas.isEmpty()) {
        modulos.add(new AiDocumentoPlano.Modulo(
            UUID.randomUUID(), null, nomeModulo, ++ordemModulo, List.copyOf(paginas)));
      }
    }
    if (!utilizados.equals(paginasOriginais.keySet())) {
      throw new IllegalStateException("A IA não devolveu todas as páginas do documento.");
    }

    String projetoNome = nomesSugeridos.getFirst();
    List<AiDocumentoPlano.Modulo> contextualizados = modulos.stream()
        .map(modulo -> new AiDocumentoPlano.Modulo(
            modulo.id(),
            null,
            modulo.nome(),
            modulo.ordem(),
            modulo.paginas().stream()
                .map(pagina -> new AiDocumentoPlano.Pagina(
                    pagina.id(),
                    pagina.titulo(),
                    pagina.ordem(),
                    atualizarCabecalho(
                        pagina.briefing(), projetoNome, modulo.nome(), pagina.titulo()),
                    pagina.templateId(),
                    pagina.templateCodigo(),
                    pagina.templateNome(),
                    pagina.confiancaTemplate(),
                    pagina.motivoTemplate(),
                    pagina.status(),
                    pagina.paginaId(),
                    pagina.sessaoId(),
                    pagina.erroMensagem()))
                .toList()))
        .toList();
    List<AiDocumentoPlano.Sugestao> sugestoes = lerSugestoes(
        resposta.path("sugestoes"), contextualizados, paginasOriginais.keySet());
    return new AiDocumentoPlano(
        projetoNome,
        descricao,
        base.projetoId(),
        base.clienteId(),
        false,
        contextualizados,
        nomesSugeridos,
        AiDocumentoAnaliseOrigem.LLM,
        "Estrutura, nomes e ordem refinados semanticamente pela IA.",
        completion.tokensEntrada(),
        completion.tokensSaida(),
        sugestoes);
  }

  private JsonNode schema() {
    try {
      return objectMapper.readTree("""
          {
            "type":"object",
            "additionalProperties":false,
            "required":["projetoNomes","projetoDescricao","modulos","sugestoes"],
            "properties":{
              "projetoNomes":{"type":"array","minItems":1,"maxItems":3,"items":{"type":"string"}},
              "projetoDescricao":{"type":"string"},
              "modulos":{"type":"array","minItems":1,"maxItems":30,"items":{
                "type":"object","additionalProperties":false,"required":["nome","paginas"],
                "properties":{
                  "nome":{"type":"string"},
                  "paginas":{"type":"array","minItems":1,"items":{
                    "type":"object","additionalProperties":false,"required":["paginaId","titulo"],
                    "properties":{"paginaId":{"type":"string"},"titulo":{"type":"string"}}
                  }}
                }
              }},
              "sugestoes":{"type":"array","maxItems":12,"items":{
                "type":"object","additionalProperties":false,
                "required":["tipo","titulo","justificativa","confianca","paginaOrigemId",
                  "paginaDestinoId","moduloOrigemNome","moduloDestinoNome","valorSugerido",
                  "conteudoSugerido"],
                "properties":{
                  "tipo":{"type":"string","enum":["ADICIONAR_PAGINA","RENOMEAR_PAGINA",
                    "MOVER_PAGINA","MESCLAR_PAGINAS","RENOMEAR_MODULO"]},
                  "titulo":{"type":"string"},
                  "justificativa":{"type":"string"},
                  "confianca":{"type":"number","minimum":0,"maximum":1},
                  "paginaOrigemId":{"type":["string","null"]},
                  "paginaDestinoId":{"type":["string","null"]},
                  "moduloOrigemNome":{"type":["string","null"]},
                  "moduloDestinoNome":{"type":["string","null"]},
                  "valorSugerido":{"type":["string","null"]},
                  "conteudoSugerido":{"type":["string","null"]}
                }
              }}
            }
          }
          """);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Schema interno da análise semântica inválido.", ex);
    }
  }

  private JsonNode lerJson(String conteudo) {
    try {
      String limpo = conteudo == null ? "" : conteudo.trim();
      if (limpo.startsWith("```")) {
        limpo = limpo.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
      }
      return objectMapper.readTree(limpo);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("A IA não retornou JSON válido para o plano.", ex);
    }
  }

  private List<String> lerNomes(JsonNode node, String padrao) {
    List<String> nomes = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        String nome = textoLimitado(item.asText(), 150, null);
        if (nome != null && nomes.stream().noneMatch(nome::equalsIgnoreCase)) nomes.add(nome);
        if (nomes.size() == 3) break;
      }
    }
    if (nomes.isEmpty()) nomes.add(textoLimitado(padrao, 150, "Manual importado"));
    return List.copyOf(nomes);
  }

  private List<AiDocumentoPlano.Sugestao> lerSugestoes(
      JsonNode node,
      List<AiDocumentoPlano.Modulo> modulos,
      Set<UUID> paginasConhecidas) {
    if (!node.isArray() || node.isEmpty()) return List.of();
    List<AiDocumentoPlano.Sugestao> sugestoes = new ArrayList<>();
    Set<String> unicas = new HashSet<>();
    for (JsonNode item : node) {
      if (sugestoes.size() >= MAXIMO_SUGESTOES) break;
      try {
        AiDocumentoSugestaoTipo tipo = AiDocumentoSugestaoTipo.valueOf(item.path("tipo").asText());
        UUID paginaOrigemId = uuidOpcional(item.path("paginaOrigemId"));
        UUID paginaDestinoId = uuidOpcional(item.path("paginaDestinoId"));
        UUID moduloOrigemId = moduloPorNome(modulos, textoOpcional(item.path("moduloOrigemNome")));
        UUID moduloDestinoId = moduloPorNome(modulos, textoOpcional(item.path("moduloDestinoNome")));
        String valorSugerido = textoLimitado(
            textoOpcional(item.path("valorSugerido")), 180, null);
        String conteudoSugerido = textoLimitado(
            textoOpcional(item.path("conteudoSugerido")), 2_000, null);
        if (!sugestaoValida(
            tipo,
            paginaOrigemId,
            paginaDestinoId,
            moduloOrigemId,
            moduloDestinoId,
            valorSugerido,
            paginasConhecidas)) {
          continue;
        }
        String chave = tipo + ":" + paginaOrigemId + ":" + paginaDestinoId + ":"
            + moduloOrigemId + ":" + moduloDestinoId + ":" + valorSugerido;
        if (!unicas.add(chave.toLowerCase())) continue;
        sugestoes.add(new AiDocumentoPlano.Sugestao(
            UUID.randomUUID(),
            tipo,
            textoLimitado(item.path("titulo").asText(), 180, rotuloPadrao(tipo)),
            textoLimitado(item.path("justificativa").asText(), 800, "Melhoria identificada pela IA."),
            item.path("confianca").asDouble(0.5),
            AiDocumentoSugestaoStatus.PENDENTE,
            paginaOrigemId,
            paginaDestinoId,
            moduloOrigemId,
            moduloDestinoId,
            valorSugerido,
            conteudoSugerido));
      } catch (RuntimeException ex) {
        // Uma sugestão inválida não deve descartar a estrutura principal já validada.
      }
    }
    return List.copyOf(sugestoes);
  }

  private boolean sugestaoValida(
      AiDocumentoSugestaoTipo tipo,
      UUID paginaOrigemId,
      UUID paginaDestinoId,
      UUID moduloOrigemId,
      UUID moduloDestinoId,
      String valorSugerido,
      Set<UUID> paginasConhecidas) {
    boolean origemValida = paginaOrigemId != null && paginasConhecidas.contains(paginaOrigemId);
    return switch (tipo) {
      case ADICIONAR_PAGINA -> moduloDestinoId != null && valorSugerido != null;
      case RENOMEAR_PAGINA -> origemValida && valorSugerido != null;
      case MOVER_PAGINA -> origemValida && moduloDestinoId != null;
      case MESCLAR_PAGINAS -> origemValida
          && paginaDestinoId != null
          && paginasConhecidas.contains(paginaDestinoId)
          && !paginaOrigemId.equals(paginaDestinoId);
      case RENOMEAR_MODULO -> moduloOrigemId != null && valorSugerido != null;
    };
  }

  private UUID moduloPorNome(List<AiDocumentoPlano.Modulo> modulos, String nome) {
    if (nome == null) return null;
    return modulos.stream()
        .filter(modulo -> modulo.nome().equalsIgnoreCase(nome.trim()))
        .map(AiDocumentoPlano.Modulo::id)
        .findFirst()
        .orElse(null);
  }

  private UUID uuidOpcional(JsonNode node) {
    String valor = textoOpcional(node);
    return valor == null ? null : uuid(valor);
  }

  private String textoOpcional(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) return null;
    String valor = node.asText().trim();
    return valor.isBlank() ? null : valor;
  }

  private String rotuloPadrao(AiDocumentoSugestaoTipo tipo) {
    return switch (tipo) {
      case ADICIONAR_PAGINA -> "Adicionar página ausente";
      case RENOMEAR_PAGINA -> "Renomear página";
      case MOVER_PAGINA -> "Mover página";
      case MESCLAR_PAGINAS -> "Mesclar páginas duplicadas";
      case RENOMEAR_MODULO -> "Renomear módulo";
    };
  }

  private String atualizarCabecalho(String briefing, String projeto, String modulo, String titulo) {
    String conteudo = briefing == null ? "" : briefing;
    conteudo = conteudo.replaceFirst(
        "(?m)^# Projeto:.*$", Matcher.quoteReplacement("# Projeto: " + projeto));
    conteudo = conteudo.replaceFirst(
        "(?m)^## Módulo:.*$", Matcher.quoteReplacement("## Módulo: " + modulo));
    return conteudo.replaceFirst(
        "(?m)^### Página:.*$", Matcher.quoteReplacement("### Página: " + titulo));
  }

  private String trechoConteudo(String briefing) {
    String texto = briefing == null
        ? ""
        : briefing.replaceFirst("(?s)^# Projeto:.*?### Página:.*?\\R+", "").trim();
    return texto.length() <= MAXIMO_TRECHO_PAGINA
        ? texto
        : texto.substring(0, MAXIMO_TRECHO_PAGINA).trim() + "…";
  }

  private UUID uuid(String valor) {
    try {
      return UUID.fromString(valor);
    } catch (RuntimeException ex) {
      throw new IllegalStateException("A IA retornou um identificador de página inválido.", ex);
    }
  }

  private String textoLimitado(String valor, int maximo, String padrao) {
    String texto = valor == null || valor.isBlank() ? padrao : valor.trim();
    if (texto == null) return null;
    return texto.length() <= maximo ? texto : texto.substring(0, maximo).trim();
  }

  private String escrever(Object valor) {
    try {
      return objectMapper.writeValueAsString(valor);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Não foi possível montar o manifesto da análise.", ex);
    }
  }
}

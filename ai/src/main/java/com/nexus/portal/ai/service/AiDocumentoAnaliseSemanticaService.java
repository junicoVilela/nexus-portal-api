package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.entity.AiDocumentoAnaliseOrigem;
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
        completion.tokensSaida());
  }

  private JsonNode schema() {
    try {
      return objectMapper.readTree("""
          {
            "type":"object",
            "additionalProperties":false,
            "required":["projetoNomes","projetoDescricao","modulos"],
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

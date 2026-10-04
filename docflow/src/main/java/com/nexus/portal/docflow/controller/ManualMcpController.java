package com.nexus.portal.docflow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexus.portal.docflow.service.ManualBusca;
import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import com.nexus.portal.docflow.service.ManualLeitorService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servidor MCP do manual (INT-504), transporte Streamable HTTP sem streaming: cada POST é uma
 * mensagem JSON-RPC e a resposta vem em JSON.
 *
 * <p>Autenticação: {@code Authorization: Bearer <chave do manual nxm_… ou link de prévia>}. O corpus é
 * o manual <b>vigente</b> do cliente — a última publicação concluída —, nunca rascunho.
 *
 * <pre>
 * claude mcp add --transport http manual-acme https://&lt;api&gt;/api/v1/docflow/mcp \
 *   --header "Authorization: Bearer &lt;token&gt;"
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/docflow/mcp")
public class ManualMcpController {

  static final String VERSAO_PROTOCOLO = "2025-06-18";
  private static final int LIMITE_PADRAO = 5;
  private static final int LIMITE_MAXIMO = 10;

  private final ManualLeitorService leitorService;
  private final ObjectMapper json;

  public ManualMcpController(ManualLeitorService leitorService, ObjectMapper json) {
    this.leitorService = leitorService;
    this.json = json;
  }

  /** Sem stream iniciado pelo servidor: o GET da especificação responde 405. */
  @GetMapping
  public ResponseEntity<Void> semStream() {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
  }

  /**
   * Corpo como texto: o JSON-RPC é montado com o Jackson do projeto, independente do conversor
   * HTTP do Spring.
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> mensagem(
      @RequestHeader(value = "Authorization", required = false) String autorizacao,
      @RequestBody String corpo) throws JsonProcessingException {
    JsonNode mensagem;
    try {
      mensagem = json.readTree(corpo);
    } catch (JsonProcessingException ex) {
      return ResponseEntity.badRequest().body(json.writeValueAsString(erro(null, -32700, "JSON inválido.")));
    }
    ResponseEntity<JsonNode> resposta = responder(autorizacao, mensagem);
    return ResponseEntity.status(resposta.getStatusCode())
        .body(resposta.getBody() == null ? null : json.writeValueAsString(resposta.getBody()));
  }

  ResponseEntity<JsonNode> responder(String autorizacao, JsonNode mensagem) {
    Corpus corpus;
    try {
      corpus = corpus(autorizacao);
    } catch (NotFoundException | BusinessException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(erro(mensagem.path("id"), -32001, ex.getMessage()));
    }
    if (!mensagem.has("id")) {
      return ResponseEntity.accepted().build(); // notificação (ex.: notifications/initialized)
    }
    JsonNode id = mensagem.get("id");
    String metodo = mensagem.path("method").asText();
    return ResponseEntity.ok(switch (metodo) {
      case "initialize" -> resultado(id, inicializar(corpus));
      case "ping" -> resultado(id, json.createObjectNode());
      case "tools/list" -> resultado(id, ferramentas());
      case "tools/call" -> resultado(id, chamar(corpus, mensagem.path("params")));
      default -> erro(id, -32601, "Método não suportado: " + metodo);
    });
  }

  private Corpus corpus(String autorizacao) {
    if (autorizacao == null || !autorizacao.startsWith("Bearer ") || autorizacao.length() <= 7) {
      throw new NotFoundException("Informe o token de leitura do manual em Authorization: Bearer <token>.");
    }
    // Chave de integração (nxm_…) ou link de prévia; agentes não mandam Origin.
    return leitorService.corpusVigente(autorizacao.substring(7).strip(), null);
  }

  private ObjectNode inicializar(Corpus corpus) {
    ObjectNode resultado = json.createObjectNode();
    resultado.put("protocolVersion", VERSAO_PROTOCOLO);
    resultado.putObject("capabilities").putObject("tools");
    ObjectNode servidor = resultado.putObject("serverInfo");
    servidor.put("name", "nexus-docflow-manual");
    servidor.put("version", corpus.versao());
    resultado.put("instructions", corpus.rotulo() + ": manual de uso publicado, uma página por tela do sistema."
        + " Use buscar para achar o assunto e paginaPorCodigo para ler a tela inteira."
        + " Cite o código da tela ao responder. Se o manual não cobrir o assunto, diga que não encontrou.");
    return resultado;
  }

  private ObjectNode ferramentas() {
    ObjectNode resultado = json.createObjectNode();
    ArrayNode tools = resultado.putArray("tools");
    ObjectNode buscar = schema(tools, "buscar",
        "Busca no manual publicado e devolve os trechos mais relevantes, com código da tela e caminho.");
    ((ObjectNode) buscar.get("properties")).putObject("consulta")
        .put("type", "string").put("description", "Pergunta ou palavras-chave");
    ((ObjectNode) buscar.get("properties")).putObject("limite")
        .put("type", "integer").put("minimum", 1).put("maximum", LIMITE_MAXIMO);
    buscar.putArray("required").add("consulta");
    ObjectNode pagina = schema(tools, "paginaPorCodigo",
        "Devolve a página inteira de uma tela do manual (Markdown), pelo código da tela (ex.: PED-001).");
    ((ObjectNode) pagina.get("properties")).putObject("codigoTela").put("type", "string");
    pagina.putArray("required").add("codigoTela");
    schema(tools, "listarTelas", "Lista as telas do manual: código, título e caminho.");
    return resultado;
  }

  /** Cria a ferramenta e devolve o {@code inputSchema} dela, já com {@code properties}. */
  private static ObjectNode schema(ArrayNode tools, String nome, String descricao) {
    ObjectNode tool = tools.addObject();
    tool.put("name", nome);
    tool.put("description", descricao);
    ObjectNode schema = tool.putObject("inputSchema");
    schema.put("type", "object");
    schema.putObject("properties");
    return schema;
  }

  private ObjectNode chamar(Corpus corpus, JsonNode params) {
    JsonNode args = params.path("arguments");
    return switch (params.path("name").asText()) {
      case "buscar" -> buscar(corpus, args.path("consulta").asText(""),
          Math.clamp(args.path("limite").asInt(LIMITE_PADRAO), 1, LIMITE_MAXIMO));
      case "paginaPorCodigo" -> pagina(corpus, args.path("codigoTela").asText(""));
      case "listarTelas" -> texto(listar(corpus), false);
      default -> texto("Ferramenta desconhecida: " + params.path("name").asText(), true);
    };
  }

  private ObjectNode buscar(Corpus corpus, String consulta, int limite) {
    var resposta = ManualBusca.buscar(corpus, consulta, limite);
    if (!resposta.encontrou()) {
      return texto("Nada no " + corpus.rotulo() + " responde a \"" + consulta + "\". Não invente: diga que o "
          + "manual não cobre o assunto.", false);
    }
    StringBuilder out = new StringBuilder();
    for (var resultado : resposta.resultados()) {
      Documento doc = resultado.secao().documento();
      out.append("### ").append(doc.titulo());
      if (resultado.secao().titulo() != null) {
        out.append(" — ").append(resultado.secao().titulo());
      }
      out.append("\nTela: ").append(doc.codigoTela()).append(" · ").append(doc.caminho());
      if (doc.url() != null) {
        out.append(" · ").append(doc.url());
      }
      out.append("\n\n").append(resultado.secao().texto()).append("\n\n");
    }
    return texto(out.toString().strip(), false);
  }

  private ObjectNode pagina(Corpus corpus, String codigoTela) {
    Documento doc = corpus.documentos().entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(codigoTela.strip()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
    if (doc == null) {
      return texto("A tela " + codigoTela + " não está no " + corpus.rotulo() + ".", true);
    }
    return texto("Tela: " + doc.codigoTela() + " · " + doc.caminho() + "\n\n" + doc.markdown(), false);
  }

  private static String listar(Corpus corpus) {
    StringBuilder out = new StringBuilder(corpus.rotulo()).append("\n\n");
    corpus.documentos().values().forEach(doc ->
        out.append("- ").append(doc.codigoTela()).append(": ").append(doc.titulo())
            .append(" (").append(doc.caminho()).append(")\n"));
    return out.toString().strip();
  }

  private ObjectNode texto(String texto, boolean erro) {
    ObjectNode resultado = json.createObjectNode();
    resultado.putArray("content").addObject().put("type", "text").put("text", texto);
    resultado.put("isError", erro);
    return resultado;
  }

  private ObjectNode resultado(JsonNode id, JsonNode resultado) {
    ObjectNode resposta = json.createObjectNode();
    resposta.put("jsonrpc", "2.0");
    resposta.set("id", id);
    resposta.set("result", resultado);
    return resposta;
  }

  private ObjectNode erro(JsonNode id, int codigo, String mensagem) {
    ObjectNode resposta = json.createObjectNode();
    resposta.put("jsonrpc", "2.0");
    resposta.set("id", id == null || id.isMissingNode() ? json.nullNode() : id);
    resposta.putObject("error").put("code", codigo).put("message", mensagem);
    return resposta;
  }
}

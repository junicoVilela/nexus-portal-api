package com.nexus.portal.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.service.AiPagePatch.Operacao;
import com.nexus.portal.ai.service.AiPagePatch.Tipo;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.springframework.stereotype.Service;

/**
 * Valida e aplica o {@link AiPagePatch} sobre o HTML atual da página. O modelo nunca escreve HTML:
 * altera texto de unidades editáveis, insere componentes do catálogo (renderizados aqui) ou remove
 * unidades. Operação inválida é descartada com aviso, sem derrubar a proposta.
 */
@Service
public class AiPagePatchService {

  static final int MAX_OPERACOES = 25;
  static final int MAX_INSERCOES = 3;
  static final int MAX_UNIDADES_ESBOCO = 400;
  static final int MAX_CARACTERES_ESBOCO = 30_000;

  private final ObjectMapper objectMapper;
  private final DocFlowAiBridge docFlowAiBridge;
  private final AiHtmlSanitizer htmlSanitizer;

  public AiPagePatchService(
      ObjectMapper objectMapper, DocFlowAiBridge docFlowAiBridge, AiHtmlSanitizer htmlSanitizer) {
    this.objectMapper = objectMapper;
    this.docFlowAiBridge = docFlowAiBridge;
    this.htmlSanitizer = htmlSanitizer;
  }

  /** Esboço grande demais para ir inteiro ao modelo: o autor precisa escolher uma seção. */
  public static boolean exigeSecao(AiPaginaEsboco esboco) {
    return esboco.totalUnidades() > MAX_UNIDADES_ESBOCO
        || esboco.totalCaracteres() > MAX_CARACTERES_ESBOCO;
  }

  public JsonNode schema(List<PaginaBlocoResponse> catalogo) {
    ObjectNode texto = objeto();
    propriedades(texto).set("slotId", tipo("string"));
    propriedades(texto).set("valor", tipo("string"));
    texto.set("required", array("slotId", "valor"));

    ObjectNode operacao = objeto();
    ObjectNode tipoOperacao = tipo("string");
    ArrayNode tipos = objectMapper.createArrayNode();
    for (Tipo tipo : Tipo.values()) {
      tipos.add(tipo.name());
    }
    tipoOperacao.set("enum", tipos);
    propriedades(operacao).set("tipo", tipoOperacao);
    for (String campo : List.of("unidadeId", "novoTexto", "aposSecaoId", "motivo")) {
      propriedades(operacao).set(campo, tipo("string"));
    }
    ObjectNode componenteId = tipo("string");
    ArrayNode ids = objectMapper.createArrayNode();
    catalogo.forEach(bloco -> ids.add(bloco.id()));
    componenteId.set("enum", ids);
    propriedades(operacao).set("componenteId", componenteId);
    ObjectNode textos = tipo("array");
    textos.set("items", texto);
    propriedades(operacao).set("textos", textos);
    operacao.set("required", array("tipo", "motivo"));

    ObjectNode raiz = objeto();
    propriedades(raiz).set("resumoDaMudanca", tipo("string"));
    ObjectNode operacoes = tipo("array");
    operacoes.put("maxItems", MAX_OPERACOES);
    operacoes.set("items", operacao);
    propriedades(raiz).set("operacoes", operacoes);
    raiz.set("required", array("resumoDaMudanca", "operacoes"));
    return raiz;
  }

  /**
   * Converte a resposta do modelo em patch validado. Ids das operações são atribuídos aqui
   * ({@code op1}, {@code op2}…) — os do modelo não são confiáveis.
   */
  public Interpretacao interpretar(
      JsonNode json,
      AiPaginaEsboco esboco,
      String titulo,
      String resumo,
      String secaoId,
      List<PaginaBlocoResponse> catalogo) {
    List<String> avisos = new ArrayList<>();
    Map<String, PaginaBlocoResponse> blocos = new HashMap<>();
    catalogo.forEach(bloco -> blocos.put(bloco.id(), bloco));
    List<Operacao> validas = new ArrayList<>();
    Set<String> unidadesTocadas = new HashSet<>();
    int insercoes = 0;
    int descartadas = 0;

    JsonNode lista = json == null ? null : json.path("operacoes");
    if (lista == null || !lista.isArray()) {
      avisos.add("A IA devolveu uma resposta inválida; nenhuma mudança foi proposta.");
      return new Interpretacao(new AiPagePatch(null, List.of()), avisos);
    }
    for (JsonNode item : lista) {
      if (validas.size() >= MAX_OPERACOES) {
        avisos.add("A IA propôs mais de " + MAX_OPERACOES + " mudanças; só as primeiras foram mantidas.");
        break;
      }
      Tipo tipo = tipoOperacao(item.path("tipo").asText(""));
      String motivo = texto(item, "motivo");
      String id = "op" + (validas.size() + 1);
      if (tipo == null) {
        descartadas++;
        continue;
      }
      switch (tipo) {
        case ALTERAR_TEXTO -> {
          String unidadeId = texto(item, "unidadeId");
          String novoTexto = texto(item, "novoTexto");
          String antes = textoAtual(esboco, unidadeId, titulo, resumo, secaoId);
          if (antes == null || novoTexto == null || unidadesTocadas.contains(unidadeId)) {
            descartadas++;
            continue;
          }
          if (novoTexto.equals(antes)) {
            continue;
          }
          unidadesTocadas.add(unidadeId);
          validas.add(new Operacao(id, tipo, unidadeId, antes, novoTexto, null, null, null, motivo));
        }
        case INSERIR_BLOCO -> {
          String aposSecaoId = texto(item, "aposSecaoId");
          PaginaBlocoResponse bloco = blocos.get(texto(item, "componenteId"));
          boolean secaoValida = aposSecaoId != null
              && esboco.secao(aposSecaoId).isPresent()
              && (secaoId == null || secaoId.equals(aposSecaoId));
          if (!secaoValida || bloco == null || insercoes >= MAX_INSERCOES) {
            descartadas++;
            continue;
          }
          insercoes++;
          validas.add(new Operacao(
              id, tipo, null, null, null, aposSecaoId, bloco.id(), slots(item, bloco), motivo));
        }
        case REMOVER_UNIDADE -> {
          String unidadeId = texto(item, "unidadeId");
          var unidade = esboco.unidade(unidadeId)
              .filter(u -> !u.titulo())
              .filter(u -> secaoId == null || pertence(esboco, secaoId, u.id()));
          if (unidade.isEmpty() || unidadesTocadas.contains(unidadeId)) {
            descartadas++;
            continue;
          }
          unidadesTocadas.add(unidadeId);
          validas.add(new Operacao(
              id, tipo, unidadeId, unidade.get().texto(), null, null, null, null, motivo));
        }
      }
    }
    if (descartadas > 0) {
      avisos.add(descartadas == 1
          ? "1 mudança proposta pela IA foi descartada (trecho inexistente, protegido ou fora do escopo)."
          : descartadas + " mudanças propostas pela IA foram descartadas (trechos inexistentes, "
              + "protegidos ou fora do escopo).");
    }
    if (validas.isEmpty()) {
      avisos.add("A IA não propôs mudanças aplicáveis. Tente detalhar o pedido.");
    }
    return new Interpretacao(
        new AiPagePatch(json.path("resumoDaMudanca").asText(null), validas), avisos);
  }

  /**
   * Aplica as operações aceitas sobre {@code html} — o mesmo HTML que gerou o esboço (a versão da
   * página é conferida antes). Ordem: alterações, inserções, remoções, para os ids não deslocarem.
   */
  public Resultado aplicar(
      String html, String titulo, String resumo, AiPagePatch patch, Set<String> aceitas) {
    AiPaginaEsboco esboco = AiPaginaEsboco.de(html);
    List<Operacao> operacoes = patch.operacoes().stream()
        .filter(operacao -> aceitas == null || aceitas.contains(operacao.id()))
        .toList();
    String tituloFinal = titulo;
    String resumoFinal = resumo;
    for (Operacao operacao : porTipo(operacoes, Tipo.ALTERAR_TEXTO)) {
      switch (operacao.unidadeId()) {
        case AiPagePatch.UNIDADE_TITULO -> tituloFinal = operacao.novoTexto();
        case AiPagePatch.UNIDADE_RESUMO -> resumoFinal = operacao.novoTexto();
        default -> esboco.unidade(operacao.unidadeId())
            .ifPresent(unidade -> unidade.elemento().text(operacao.novoTexto()));
      }
    }
    Map<String, Node> ultimoInserido = new LinkedHashMap<>();
    for (Operacao operacao : porTipo(operacoes, Tipo.INSERIR_BLOCO)) {
      esboco.secao(operacao.aposSecaoId()).ifPresent(secao -> {
        Node ancora = ultimoInserido.getOrDefault(secao.id(), secao.ultimoNo());
        List<Node> novos = Jsoup.parseBodyFragment(
                docFlowAiBridge.renderizarBloco(operacao.componenteId(), operacao.textos()))
            .body().childNodes();
        for (Node novo : List.copyOf(novos)) {
          ancora.after(novo);
          ancora = novo;
        }
        ultimoInserido.put(secao.id(), ancora);
      });
    }
    for (Operacao operacao : porTipo(operacoes, Tipo.REMOVER_UNIDADE)) {
      esboco.unidade(operacao.unidadeId()).ifPresent(unidade -> unidade.elemento().remove());
    }
    return new Resultado(
        htmlSanitizer.sanitizar(esboco.documento().body().html()), tituloFinal, resumoFinal);
  }

  public String escrever(AiPagePatch patch) {
    try {
      return objectMapper.writeValueAsString(patch);
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao serializar o patch da página.", ex);
    }
  }

  public AiPagePatch ler(String json) {
    try {
      return objectMapper.readValue(json, AiPagePatch.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Patch da proposta ilegível.", ex);
    }
  }

  private static String textoAtual(
      AiPaginaEsboco esboco, String unidadeId, String titulo, String resumo, String secaoId) {
    if (unidadeId == null) {
      return null;
    }
    if (AiPagePatch.UNIDADE_TITULO.equals(unidadeId)) {
      return titulo == null ? "" : titulo;
    }
    if (AiPagePatch.UNIDADE_RESUMO.equals(unidadeId)) {
      return resumo == null ? "" : resumo;
    }
    return esboco.unidade(unidadeId)
        .filter(AiPaginaEsboco.Unidade::editavel)
        .filter(unidade -> secaoId == null || pertence(esboco, secaoId, unidade.id()))
        .map(AiPaginaEsboco.Unidade::texto)
        .orElse(null);
  }

  private static boolean pertence(AiPaginaEsboco esboco, String secaoId, String unidadeId) {
    return esboco.secao(secaoId)
        .map(secao -> secao.unidades().stream().anyMatch(u -> u.id().equals(unidadeId)))
        .orElse(false);
  }

  private static Map<String, String> slots(JsonNode item, PaginaBlocoResponse bloco) {
    Set<String> permitidos = new HashSet<>();
    bloco.slots().forEach(slot -> permitidos.add(slot.id()));
    Map<String, String> textos = new LinkedHashMap<>();
    for (JsonNode texto : item.path("textos")) {
      String slotId = texto.path("slotId").asText("");
      String valor = texto.path("valor").asText("").trim();
      if (permitidos.contains(slotId) && !valor.isEmpty()) {
        textos.putIfAbsent(slotId, valor);
      }
    }
    return textos;
  }

  private static List<Operacao> porTipo(List<Operacao> operacoes, Tipo tipo) {
    return operacoes.stream().filter(operacao -> operacao.tipo() == tipo).toList();
  }

  private static Tipo tipoOperacao(String valor) {
    try {
      return Tipo.valueOf(valor);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static String texto(JsonNode item, String campo) {
    String valor = item.path(campo).asText("").trim();
    return valor.isEmpty() ? null : valor;
  }

  private ObjectNode objeto() {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("type", "object");
    node.set("properties", objectMapper.createObjectNode());
    node.put("additionalProperties", false);
    return node;
  }

  private static ObjectNode propriedades(ObjectNode node) {
    return (ObjectNode) node.get("properties");
  }

  private ObjectNode tipo(String tipo) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("type", tipo);
    return node;
  }

  private ArrayNode array(String... valores) {
    ArrayNode node = objectMapper.createArrayNode();
    for (String valor : valores) {
      node.add(valor);
    }
    return node;
  }

  public record Interpretacao(AiPagePatch patch, List<String> avisos) {}

  public record Resultado(String html, String titulo, String resumo) {}
}

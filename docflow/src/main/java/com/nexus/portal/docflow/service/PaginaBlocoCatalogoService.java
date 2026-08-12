package com.nexus.portal.docflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.docflow.dto.response.PaginaBlocoResponse;
import com.nexus.portal.docflow.dto.response.PaginaBlocoSlotResponse;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Fonte canônica da biblioteca de blocos do DocFlow.
 *
 * <p>O mesmo catálogo alimenta editor, assistente de IA e renderização. O LLM recebe somente
 * IDs e slots; o HTML confiável permanece no servidor.
 */
@Service
public class PaginaBlocoCatalogoService {

  private static final String RECURSO = "docflow/pagina-blocos.json";

  private final List<PaginaBlocoResponse> blocos;
  private final Map<String, PaginaBlocoResponse> blocosPorId;

  public PaginaBlocoCatalogoService(ObjectMapper objectMapper) {
    this.blocos = carregar(objectMapper);
    Map<String, PaginaBlocoResponse> porId = new LinkedHashMap<>();
    for (PaginaBlocoResponse bloco : blocos) {
      if (porId.putIfAbsent(bloco.id(), bloco) != null) {
        throw new IllegalStateException("Bloco duplicado no catálogo: " + bloco.id());
      }
    }
    this.blocosPorId = Map.copyOf(porId);
  }

  public List<PaginaBlocoResponse> listar() {
    return blocos;
  }

  public PaginaBlocoResponse buscar(String id) {
    PaginaBlocoResponse bloco = blocosPorId.get(id);
    if (bloco == null) {
      throw new BusinessException("Componente de página não encontrado no catálogo: " + id);
    }
    return bloco;
  }

  /**
   * Preenche apenas nós de texto conhecidos, preservando tags, classes, atributos e hierarquia.
   */
  public String renderizar(String id, Map<String, String> valores) {
    PaginaBlocoResponse bloco = buscar(id);
    Document documento = Jsoup.parseBodyFragment(bloco.html());
    List<TextNode> nos = nosTexto(documento);
    Map<String, String> seguros = valores == null ? Map.of() : valores;
    for (int indice = 0; indice < nos.size(); indice++) {
      String valor = seguros.get("t" + (indice + 1));
      if (valor != null && !valor.isBlank()) {
        nos.get(indice).text(valor.trim());
      }
    }
    return documento.body().html();
  }

  private static List<PaginaBlocoResponse> carregar(ObjectMapper objectMapper) {
    try (InputStream input = new ClassPathResource(RECURSO).getInputStream()) {
      List<PaginaBlocoRecurso> recursos = objectMapper.readValue(
          input, new TypeReference<List<PaginaBlocoRecurso>>() {});
      if (recursos.isEmpty()) {
        throw new IllegalStateException("Catálogo de blocos vazio: " + RECURSO);
      }
      return recursos.stream().map(PaginaBlocoCatalogoService::mapear).toList();
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao carregar catálogo de blocos " + RECURSO, ex);
    }
  }

  private static PaginaBlocoResponse mapear(PaginaBlocoRecurso recurso) {
    if (recurso.id() == null || recurso.id().isBlank() || recurso.html() == null
        || recurso.html().isBlank()) {
      throw new IllegalStateException("Bloco inválido no catálogo: " + recurso);
    }
    return new PaginaBlocoResponse(
        recurso.id(),
        recurso.nome(),
        recurso.descricao(),
        recurso.categoria(),
        recurso.visual(),
        recurso.html(),
        recurso.parametrizacao(),
        1,
        extrairSlots(recurso.html()));
  }

  private static List<PaginaBlocoSlotResponse> extrairSlots(String html) {
    Document documento = Jsoup.parseBodyFragment(html);
    List<TextNode> nos = nosTexto(documento);
    List<PaginaBlocoSlotResponse> slots = new ArrayList<>();
    for (int indice = 0; indice < nos.size(); indice++) {
      TextNode texto = nos.get(indice);
      Node parent = texto.parent();
      String elemento = parent == null ? "texto" : parent.nodeName();
      String classeCss = parent == null ? "" : parent.attr("class");
      slots.add(new PaginaBlocoSlotResponse(
          "t" + (indice + 1),
          elemento,
          classeCss,
          texto.text().trim()));
    }
    return List.copyOf(slots);
  }

  private static List<TextNode> nosTexto(Document documento) {
    List<TextNode> nos = new ArrayList<>();
    NodeTraversor.traverse(new NodeVisitor() {
      @Override
      public void head(Node node, int depth) {
        if (node instanceof TextNode texto && !texto.isBlank()) {
          nos.add(texto);
        }
      }

      @Override
      public void tail(Node node, int depth) {
        // Nada a fazer.
      }
    }, documento.body());
    return nos;
  }

  private record PaginaBlocoRecurso(
      String id,
      String nome,
      String descricao,
      String categoria,
      String visual,
      String html,
      String parametrizacao) {
  }
}

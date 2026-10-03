package com.nexus.portal.ai.integration.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiGithubProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** {@code GET /repos/{repo}/pulls/{n}/files} com token opcional (repositório público dispensa). */
@Component
public class AiGithubHttpClient implements AiGithubClient {

  private static final Pattern REPOSITORIO = Pattern.compile("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");
  private static final int POR_PAGINA = 100;

  private final AiGithubProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public AiGithubHttpClient(AiGithubProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<ArquivoPr> arquivos(String repositorio, int numero, int limite) {
    if (!REPOSITORIO.matcher(repositorio).matches()) {
      throw new IllegalArgumentException("Repositório inválido: " + repositorio);
    }
    List<ArquivoPr> arquivos = new ArrayList<>();
    for (int pagina = 1; arquivos.size() < limite; pagina++) {
      JsonNode lote = get("/repos/" + repositorio + "/pulls/" + numero + "/files?per_page=" + POR_PAGINA
          + "&page=" + pagina);
      for (JsonNode item : lote) {
        if (arquivos.size() >= limite) {
          break;
        }
        arquivos.add(new ArquivoPr(
            item.path("filename").asText(),
            item.path("status").asText(),
            item.path("additions").asInt(),
            item.path("deletions").asInt(),
            item.hasNonNull("patch") ? item.get("patch").asText() : null));
      }
      if (lote.size() < POR_PAGINA) {
        break;
      }
    }
    return arquivos;
  }

  private JsonNode get(String caminho) {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(properties.apiUrl() + caminho))
        .timeout(Duration.ofSeconds(15))
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .GET();
    if (properties.token() != null && !properties.token().isBlank()) {
      request.header("Authorization", "Bearer " + properties.token());
    }
    try {
      HttpResponse<String> resposta = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      if (resposta.statusCode() != 200) {
        throw new GithubIndisponivelException("GitHub respondeu " + resposta.statusCode() + " em " + caminho
            + (resposta.statusCode() == 404 ? " (repositório privado sem NEXUS_AI_GITHUB_TOKEN?)" : ""));
      }
      return objectMapper.readTree(resposta.body());
    } catch (IOException ex) {
      throw new GithubIndisponivelException("Falha ao consultar o GitHub: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new GithubIndisponivelException("Consulta ao GitHub interrompida.", ex);
    }
  }
}

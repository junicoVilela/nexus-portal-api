package com.nexus.portal.releaseorchestrator.integration.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Engine API via HTTP ({@code /v1.43}). TLS na porta 2376 ainda não é suportado —
 * use 2375 (HTTP) no host para modo REAL.
 */
@Component
@RequiredArgsConstructor
public class DockerEngineAdapter implements DockerEngineClient {

  static final String API = "/v1.43";

  private final ObjectMapper objectMapper;

  @Override
  public void ping(String baseUrl) {
    try {
      cliente(baseUrl, Duration.ofSeconds(8)).get().uri("/_ping").retrieve().toBodilessEntity();
    } catch (ResourceAccessException e) {
      throw new DockerEngineException(
          "Não conectou na API Docker em " + baseUrl
              + ". Para modo REAL sem TLS use a porta 2375 (HTTP). "
              + detalhe(e),
          e);
    } catch (RestClientResponseException e) {
      throw new DockerEngineException("API Docker recusou o ping (HTTP " + e.getStatusCode().value() + ").", e);
    }
  }

  @Override
  public void pull(String baseUrl, String imagem, String tag) {
    try {
      String body = cliente(baseUrl, Duration.ofMinutes(10))
          .post()
          .uri(uri -> uri.path(API + "/images/create").queryParam("fromImage", imagem).queryParam("tag", tag).build())
          .retrieve()
          .body(String.class);
      if (body != null && body.contains("\"error\"")) {
        throw new DockerEngineException("Falha no docker pull de " + imagem + ":" + tag + ". " + resumirErroStream(body));
      }
    } catch (DockerEngineException e) {
      throw e;
    } catch (ResourceAccessException e) {
      throw new DockerEngineException("Timeout ou falha de rede no pull de " + imagem + ":" + tag + ".", e);
    } catch (RestClientResponseException e) {
      throw new DockerEngineException(
          "Docker pull HTTP " + e.getStatusCode().value() + " para " + imagem + ":" + tag + ".", e);
    }
  }

  @Override
  public void removerSeExistir(String baseUrl, String nome) {
    try {
      cliente(baseUrl, Duration.ofSeconds(30))
          .delete()
          .uri(uri -> uri.path(API + "/containers/{nome}").queryParam("force", true).build(nome))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return;
      }
      throw new DockerEngineException("Não removeu o container " + nome + " (HTTP " + e.getStatusCode().value() + ").", e);
    }
  }

  @Override
  public String criarContainer(String baseUrl, String nome, String imagemRef, Map<String, Object> hostConfig,
      Map<String, Object> exposedPorts, Map<String, String> labels) {
    Map<String, Object> body = Map.of(
        "Image", imagemRef,
        "Labels", labels == null ? Map.of() : labels,
        "ExposedPorts", exposedPorts == null ? Map.of() : exposedPorts,
        "HostConfig", hostConfig == null ? Map.of() : hostConfig);
    try {
      JsonNode json = cliente(baseUrl, Duration.ofSeconds(30))
          .post()
          .uri(uri -> uri.path(API + "/containers/create").queryParam("name", nome).build())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(JsonNode.class);
      if (json == null || json.path("Id").isMissingNode()) {
        throw new DockerEngineException("Docker não devolveu o id do container " + nome + ".");
      }
      return json.path("Id").asText();
    } catch (RestClientResponseException e) {
      throw new DockerEngineException(
          "Não criou o container " + nome + " (HTTP " + e.getStatusCode().value() + "). " + corpo(e), e);
    }
  }

  @Override
  public void iniciar(String baseUrl, String nomeOuId) {
    try {
      cliente(baseUrl, Duration.ofSeconds(30))
          .post()
          .uri(API + "/containers/{id}/start", nomeOuId)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 304) {
        return;
      }
      throw new DockerEngineException(
          "Não iniciou o container " + nomeOuId + " (HTTP " + e.getStatusCode().value() + "). " + corpo(e), e);
    }
  }

  @Override
  public void parar(String baseUrl, String nomeOuId) {
    try {
      cliente(baseUrl, Duration.ofSeconds(45))
          .post()
          .uri(API + "/containers/{id}/stop", nomeOuId)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 304) {
        return;
      }
      throw new DockerEngineException(
          "Não parou o container " + nomeOuId + " (HTTP " + e.getStatusCode().value() + "). " + corpo(e), e);
    }
  }

  private RestClient cliente(String baseUrl, Duration readTimeout) {
    var factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(readTimeout);
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .build();
  }

  private String resumirErroStream(String body) {
    try {
      for (String line : body.split("\n")) {
        if (line.isBlank()) {
          continue;
        }
        JsonNode node = objectMapper.readTree(line);
        if (node.hasNonNull("error")) {
          return node.get("error").asText();
        }
      }
    } catch (Exception ignored) {
      return body.length() > 240 ? body.substring(0, 240) : body;
    }
    return body.length() > 240 ? body.substring(0, 240) : body;
  }

  private static String corpo(RestClientResponseException e) {
    String body = e.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      return "";
    }
    return body.length() > 240 ? body.substring(0, 240) : body;
  }

  private static String detalhe(ResourceAccessException e) {
    Throwable causa = e.getMostSpecificCause();
    return causa != null && causa.getMessage() != null ? causa.getMessage() : e.getMessage();
  }
}

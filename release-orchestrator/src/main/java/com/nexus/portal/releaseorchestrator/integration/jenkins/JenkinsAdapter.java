package com.nexus.portal.releaseorchestrator.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP mínimo para a API do Jenkins (F2.5). Cobre:
 *
 * <ul>
 *   <li>Buscar info do job (último build).</li>
 *   <li>Buscar último build específico para mostrar status na UI.</li>
 * </ul>
 *
 * Auth: Basic com user + API token. Spec: docs/release-orchestrator/09 §4.3.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsAdapter {

  /**
   * Busca o último build do job. Retorna empty quando o job não tem builds
   * ainda. Lança {@link JenkinsException} para 401/403/404/timeouts etc.
   */
  public Optional<JenkinsBuildInfo> ultimoBuild(String jenkinsUrl, String job, String user,
      String token) {
    String path = String.format("/job/%s/lastBuild/api/json?tree=number,result,building,timestamp,duration,url",
        encode(job));
    try {
      JenkinsBuildDto dto = cliente(jenkinsUrl, user, token).get().uri(path).retrieve()
          .body(JenkinsBuildDto.class);
      if (dto == null) return Optional.empty();
      return Optional.of(new JenkinsBuildInfo(dto.number(), dto.result(), dto.building(),
          dto.timestamp(), dto.duration(), dto.url()));
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return Optional.empty();
      throw new JenkinsException(mensagemAmigavel(e), e.getStatusCode().value(), e);
    }
  }

  /** Verifica que o job existe (200 em /api/json). */
  public boolean jobExiste(String jenkinsUrl, String job, String user, String token) {
    String path = "/job/" + encode(job) + "/api/json?tree=name";
    try {
      cliente(jenkinsUrl, user, token).get().uri(path).retrieve().toBodilessEntity();
      return true;
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return false;
      throw new JenkinsException(mensagemAmigavel(e), e.getStatusCode().value(), e);
    }
  }

  private RestClient cliente(String baseUrl, String user, String token) {
    String basic = Base64.getEncoder().encodeToString(
        ((user == null ? "" : user) + ":" + (token == null ? "" : token))
            .getBytes(StandardCharsets.UTF_8));
    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .build();
  }

  private String mensagemAmigavel(HttpStatusCodeException e) {
    return switch (e.getStatusCode().value()) {
      case 401 -> "Credencial Jenkins inválida (verifique usuário e API token).";
      case 403 -> "Usuário Jenkins sem permissão para acessar o job.";
      case 404 -> "Job não encontrado.";
      default -> "Jenkins retornou erro " + e.getStatusCode().value() + ".";
    };
  }

  private String encode(String job) {
    // Jenkins aceita o nome do job direto na URL; barras viram /job/.../job/...
    // Aqui escapamos só espaços, que são o caso mais comum de quebra.
    return job == null ? "" : job.replace(" ", "%20");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JenkinsBuildDto(int number, String result, boolean building, long timestamp,
      long duration, String url) {}
}

package com.nexus.portal.releaseorchestrator.integration.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP mínimo para a API do Jenkins (F2.5). Cobre:
 *
 * <ul>
 *   <li>Buscar info do job (último build).</li>
 *   <li>Disparar {@code /buildWithParameters} (com fallback para {@code /build}).</li>
 * </ul>
 *
 * Auth: Basic com user + API token. Token de API dispensa crumb; senha
 * exige crumb + cookie de sessão. Spec: docs/release-orchestrator/09 §4.3.
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
    return consultarBuild(jenkinsUrl, job, user, token, "lastBuild");
  }

  public Optional<JenkinsBuildInfo> consultarBuild(String jenkinsUrl, String job, String user,
      String token, int numero) {
    return consultarBuild(jenkinsUrl, job, user, token, String.valueOf(numero));
  }

  public List<JenkinsArtifact> listarArtefatos(String jenkinsUrl, String job, String user,
      String token, int numero) {
    String path = jobPath(job) + "/" + numero
        + "/api/json?tree=artifacts[fileName,relativePath]";
    try {
      JenkinsBuildDto dto = cliente(jenkinsUrl, user, token, null).get().uri(path).retrieve()
          .body(JenkinsBuildDto.class);
      if (dto == null || dto.artifacts() == null) return List.of();
      return dto.artifacts().stream()
          .filter(a -> a != null && a.fileName() != null && !a.fileName().isBlank())
          .map(a -> new JenkinsArtifact(a.fileName(), a.relativePath()))
          .toList();
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return List.of();
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()), e.getStatusCode().value(), e);
    }
  }

  public byte[] baixarArtefato(String jenkinsUrl, String job, String user, String token,
      int numero, String relativePath) {
    String path = jobPath(job) + "/" + numero + "/artifact/"
        + encodeArtifactPath(relativePath);
    try {
      byte[] body = cliente(jenkinsUrl, user, token, null).get().uri(path)
          .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
          .retrieve()
          .body(byte[].class);
      if (body == null) {
        throw new JenkinsException("Artefato vazio em " + relativePath + ".", 0);
      }
      return body;
    } catch (HttpStatusCodeException e) {
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()), e.getStatusCode().value(), e);
    } catch (ResourceAccessException e) {
      throw new JenkinsException("Não foi possível conectar ao Jenkins.", 0, e);
    }
  }

  public Optional<JenkinsQueueItem> consultarFila(String jenkinsUrl, String user, String token,
      String queueUrl) {
    String path = filaPath(jenkinsUrl, queueUrl);
    try {
      JenkinsQueueDto dto = cliente(jenkinsUrl, user, token, null).get().uri(path).retrieve()
          .body(JenkinsQueueDto.class);
      if (dto == null) return Optional.empty();
      Integer numero = dto.executable() == null ? null : dto.executable().number();
      String execUrl = dto.executable() == null ? null : dto.executable().url();
      return Optional.of(new JenkinsQueueItem(Boolean.TRUE.equals(dto.cancelled()), numero, execUrl));
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return Optional.empty();
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()), e.getStatusCode().value(), e);
    } catch (ResourceAccessException e) {
      throw new JenkinsException("Não foi possível conectar ao Jenkins.", 0, e);
    }
  }

  private Optional<JenkinsBuildInfo> consultarBuild(String jenkinsUrl, String job, String user,
      String token, String buildRef) {
    String path = jobPath(job) + "/" + buildRef
        + "/api/json?tree=number,result,building,timestamp,duration,url";
    try {
      JenkinsBuildDto dto = cliente(jenkinsUrl, user, token, null).get().uri(path).retrieve()
          .body(JenkinsBuildDto.class);
      if (dto == null) return Optional.empty();
      return Optional.of(new JenkinsBuildInfo(dto.number(), dto.result(), dto.building(),
          dto.timestamp(), dto.duration(), dto.url()));
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return Optional.empty();
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()), e.getStatusCode().value(), e);
    }
  }

  static String encodeArtifactPath(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) return "";
    StringBuilder sb = new StringBuilder();
    for (String parte : relativePath.replace('\\', '/').split("/")) {
      if (parte.isBlank()) continue;
      if (sb.length() > 0) sb.append('/');
      sb.append(URLEncoder.encode(parte, StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return sb.toString();
  }

  static String filaPath(String jenkinsUrl, String queueUrl) {
    if (queueUrl == null || queueUrl.isBlank()) {
      throw new JenkinsException("Fila Jenkins sem URL.", 0);
    }
    String raw = queueUrl.trim();
    String base = jenkinsUrl == null ? "" : jenkinsUrl.replaceAll("/+$", "");
    if (raw.startsWith("http://") || raw.startsWith("https://")) {
      if (!base.isBlank() && raw.startsWith(base)) {
        raw = raw.substring(base.length());
      } else {
        int q = raw.indexOf("/queue/");
        raw = q >= 0 ? raw.substring(q) : raw;
      }
    }
    if (!raw.startsWith("/")) {
      raw = "/" + raw;
    }
    if (!raw.endsWith("/")) {
      raw = raw + "/";
    }
    return raw + "api/json?tree=cancelled,executable[number,url]";
  }

  /** Verifica que o job existe (200 em /api/json). */
  public boolean jobExiste(String jenkinsUrl, String job, String user, String token) {
    String path = jobPath(job) + "/api/json?tree=name";
    try {
      cliente(jenkinsUrl, user, token, null).get().uri(path).retrieve().toBodilessEntity();
      return true;
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() == 404) return false;
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()), e.getStatusCode().value(), e);
    }
  }

  /**
   * Enfileira um build. Tenta {@code buildWithParameters}; se o job não for
   * parametrizado (400/404), cai em {@code /build}.
   */
  public JenkinsQueueInfo dispararBuild(String jenkinsUrl, String job, String user, String token,
      Map<String, String> parametros) {
    SessionCookies cookies = new SessionCookies();
    RestClient client = cliente(jenkinsUrl, user, token, cookies);
    Optional<Crumb> crumb = buscarCrumb(client);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    if (parametros != null) {
      parametros.forEach((k, v) -> {
        if (k != null && !k.isBlank() && v != null) form.add(k, v);
      });
    }
    String path = jobPath(job);
    try {
      return postBuild(client, path + "/buildWithParameters", form, crumb);
    } catch (JenkinsException e) {
      if (e.status() == 400 || e.status() == 404) {
        log.info("Job {} sem parâmetros (HTTP {}) — tentando /build.", job, e.status());
        return postBuild(client, path + "/build", form, crumb);
      }
      throw e;
    }
  }

  static String jobPath(String job) {
    if (job == null || job.isBlank()) return "/job/";
    StringBuilder sb = new StringBuilder();
    for (String parte : job.replace('\\', '/').split("/")) {
      if (parte.isBlank()) continue;
      sb.append("/job/").append(URLEncoder.encode(parte.trim(), StandardCharsets.UTF_8)
          .replace("+", "%20"));
    }
    return sb.isEmpty() ? "/job/" : sb.toString();
  }

  private JenkinsQueueInfo postBuild(RestClient client, String path,
      MultiValueMap<String, String> form, Optional<Crumb> crumb) {
    try {
      return client.post()
          .uri(path)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .headers(h -> crumb.ifPresent(c -> h.set(c.header(), c.value())))
          .body(form)
          .exchange((req, res) -> {
            int status = res.getStatusCode().value();
            if (status == 201 || status == 200 || status == 302) {
              var loc = res.getHeaders().getLocation();
              return new JenkinsQueueInfo(loc == null ? null : loc.toString());
            }
            throw new JenkinsException(mensagemAmigavel(status), status);
          });
    } catch (JenkinsException e) {
      throw e;
    } catch (ResourceAccessException e) {
      throw new JenkinsException("Não foi possível conectar ao Jenkins.", 0, e);
    } catch (HttpStatusCodeException e) {
      throw new JenkinsException(mensagemAmigavel(e.getStatusCode().value()),
          e.getStatusCode().value(), e);
    }
  }

  private Optional<Crumb> buscarCrumb(RestClient client) {
    try {
      return client.get().uri("/crumbIssuer/api/json").exchange((req, res) -> {
        int status = res.getStatusCode().value();
        if (status == 404) return Optional.empty();
        if (status >= 400) {
          throw new JenkinsException(mensagemAmigavel(status), status);
        }
        CrumbDto dto = res.bodyTo(CrumbDto.class);
        if (dto == null || dto.crumb() == null || dto.crumb().isBlank()) {
          return Optional.empty();
        }
        String header = dto.crumbRequestField() == null || dto.crumbRequestField().isBlank()
            ? "Jenkins-Crumb" : dto.crumbRequestField();
        return Optional.of(new Crumb(header, dto.crumb()));
      });
    } catch (JenkinsException e) {
      throw e;
    } catch (ResourceAccessException e) {
      throw new JenkinsException("Não foi possível conectar ao Jenkins.", 0, e);
    } catch (RuntimeException e) {
      log.debug("Crumb Jenkins indisponível (seguindo sem CSRF): {}", e.getMessage());
      return Optional.empty();
    }
  }

  private RestClient cliente(String baseUrl, String user, String token, SessionCookies cookies) {
    String basic = Base64.getEncoder().encodeToString(
        ((user == null ? "" : user) + ":" + (token == null ? "" : token))
            .getBytes(StandardCharsets.UTF_8));
    var builder = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
        .defaultHeader(HttpHeaders.ACCEPT, "application/json");
    if (cookies != null) {
      builder.requestInterceptor(cookies);
    }
    return builder.build();
  }

  private String mensagemAmigavel(int status) {
    return switch (status) {
      case 401 -> "Credencial Jenkins inválida (verifique usuário e API token).";
      case 403 -> "Usuário Jenkins sem permissão para acessar o job.";
      case 404 -> "Job não encontrado.";
      default -> "Jenkins retornou erro " + status + ".";
    };
  }

  static final class SessionCookies implements ClientHttpRequestInterceptor {
    private final List<String> cookies = new ArrayList<>();

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
        ClientHttpRequestExecution execution) throws java.io.IOException {
      if (!cookies.isEmpty()) {
        request.getHeaders().addAll(HttpHeaders.COOKIE, cookies);
      }
      ClientHttpResponse response = execution.execute(request, body);
      List<String> setCookie = response.getHeaders().get(HttpHeaders.SET_COOKIE);
      if (setCookie != null && !setCookie.isEmpty()) {
        cookies.clear();
        for (String c : setCookie) {
          int cut = c.indexOf(';');
          cookies.add(cut < 0 ? c : c.substring(0, cut));
        }
      }
      return response;
    }
  }

  private record Crumb(String header, String value) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record CrumbDto(String crumb, @JsonProperty("crumbRequestField") String crumbRequestField) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JenkinsBuildDto(int number, String result, boolean building, long timestamp,
      long duration, String url, List<JenkinsArtifactDto> artifacts) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JenkinsArtifactDto(String fileName, String relativePath) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JenkinsQueueDto(Boolean cancelled, JenkinsExecutableDto executable) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JenkinsExecutableDto(Integer number, String url) {}

  /** Exposto para montar os parâmetros do job sem acoplar o service ao adapter HTTP. */
  public static Map<String, String> parametrosPadrao(String sigla, String versao, String tag,
      String gitRef, String origem) {
    Map<String, String> p = new LinkedHashMap<>();
    if (sigla != null) p.put("PORTAL_PRODUCT_SIGLA", sigla);
    if (versao != null) p.put("RELEASE_VERSION", versao);
    if (tag != null) p.put("TAG_NAME", tag);
    if (gitRef != null) p.put("GIT_REF", gitRef);
    if (origem != null) p.put("ORIGEM", origem);
    return p;
  }
}

package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.TestarJenkinsRequest;
import com.nexus.portal.releaseorchestrator.dto.response.TestarJenkinsResponse;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsAdapter;
import com.nexus.portal.releaseorchestrator.integration.jenkins.JenkinsException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orquestração da integração Jenkins no nível do Produto. Espelha o
 * {@link GithubIntegrationService}: faz a ponte entre o cadastro do
 * produto e o {@link JenkinsAdapter}.
 */
@Service
@RequiredArgsConstructor
public class JenkinsIntegrationService {

  private final ProdutoRhService produtoService;
  private final JenkinsAdapter adapter;

  public TestarJenkinsResponse testar(UUID produtoId, TestarJenkinsRequest req) {
    ProdutoRh produto = produtoService.buscar(produtoId);
    String url = req.temOverride() && req.jenkinsUrl() != null && !req.jenkinsUrl().isBlank()
        ? req.jenkinsUrl()
        : produto.getJenkinsUrl();
    String job = req.temOverride() && req.jenkinsJob() != null && !req.jenkinsJob().isBlank()
        ? req.jenkinsJob()
        : produto.getJenkinsJob();
    String user = (req.jenkinsUser() != null && !req.jenkinsUser().isBlank())
        ? req.jenkinsUser()
        : produto.getJenkinsUser();
    String token = (req.jenkinsToken() != null && !req.jenkinsToken().isBlank())
        ? req.jenkinsToken()
        : produto.getJenkinsToken();

    if (url == null || url.isBlank()) {
      return TestarJenkinsResponse.erro(null, job, "Configure a URL do Jenkins antes de testar.");
    }
    if (job == null || job.isBlank()) {
      return TestarJenkinsResponse.erro(url, null, "Configure o nome do job antes de testar.");
    }

    try {
      if (!adapter.jobExiste(url, job, user, token)) {
        return TestarJenkinsResponse.erro(url, job, "Job não encontrado em " + url + ".");
      }
      var ultimoBuild = adapter.ultimoBuild(url, job, user, token)
          .map(b -> new TestarJenkinsResponse.UltimoBuild(
              b.number(), b.result(), b.building(), b.timestamp(), b.duration(), b.url()))
          .orElse(null);
      return TestarJenkinsResponse.ok(url, job, ultimoBuild);
    } catch (JenkinsException e) {
      return TestarJenkinsResponse.erro(url, job, e.getMessage());
    }
  }
}

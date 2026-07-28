package br.com.softon.portal.docflow.controller;

import br.com.softon.portal.docflow.dto.request.AjudaConteudoRequest;
import br.com.softon.portal.docflow.dto.request.AjudaEventoRequest;
import br.com.softon.portal.docflow.dto.response.AjudaConteudoResponse;
import br.com.softon.portal.docflow.dto.response.AjudaMetricasResponse;
import br.com.softon.portal.docflow.service.AjudaService;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docflow/ajuda")
@RequiredArgsConstructor
public class AjudaController {

  private final AjudaService ajudaService;

  @GetMapping("/conteudos")
  @PreAuthorize(Permissoes.AJUDA_LER)
  public List<AjudaConteudoResponse> listar(
      @RequestParam(required = false) String busca,
      @RequestParam(required = false) String rota) {
    return ajudaService.listar(busca, rota, false).stream().map(AjudaConteudoResponse::from).toList();
  }

  @GetMapping("/conteudos/admin")
  @PreAuthorize(Permissoes.AJUDA_EDITAR)
  public List<AjudaConteudoResponse> listarAdministracao() {
    return ajudaService.listar(null, null, true).stream().map(AjudaConteudoResponse::from).toList();
  }

  @PostMapping("/conteudos")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.AJUDA_CRIAR)
  public AjudaConteudoResponse criar(@Valid @RequestBody AjudaConteudoRequest request) {
    return AjudaConteudoResponse.from(ajudaService.criar(request));
  }

  @PutMapping("/conteudos/{id}")
  @PreAuthorize(Permissoes.AJUDA_EDITAR)
  public AjudaConteudoResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody AjudaConteudoRequest request) {
    return AjudaConteudoResponse.from(ajudaService.atualizar(id, request));
  }

  @DeleteMapping("/conteudos/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.AJUDA_EXCLUIR)
  public void excluir(@PathVariable UUID id) {
    ajudaService.excluir(id);
  }

  @PostMapping("/eventos")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(Permissoes.AJUDA_LER)
  public void registrar(@Valid @RequestBody AjudaEventoRequest request, Principal principal) {
    ajudaService.registrar(request, principal == null ? null : principal.getName());
  }

  @GetMapping("/metricas")
  @PreAuthorize(Permissoes.AJUDA_EDITAR)
  public AjudaMetricasResponse metricas() {
    return ajudaService.metricas();
  }
}

package br.com.softon.rbac.controller;

import br.com.softon.rbac.dto.request.PoliticaSenhaRequest;
import br.com.softon.rbac.dto.response.PoliticaSenhaResponse;
import br.com.softon.rbac.service.PoliticaSenhaService;
import br.com.softon.portal.shared.security.Permissoes;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/politica-senha")
public class PoliticaSenhaController {

  private final PoliticaSenhaService service;

  @GetMapping
  @PreAuthorize(Permissoes.USUARIO_LER)
  public PoliticaSenhaResponse atual() {
    return PoliticaSenhaResponse.from(service.atual());
  }

  @PutMapping
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public PoliticaSenhaResponse atualizar(@Valid @RequestBody PoliticaSenhaRequest request,
      Principal principal) {
    return PoliticaSenhaResponse.from(
        service.atualizar(
            request.tamanhoMinimo(),
            request.exigirMaiuscula(),
            request.exigirMinuscula(),
            request.exigirNumero(),
            request.exigirEspecial(),
            request.expiraSenhaDias(),
            request.quantidadeHistorico(),
            request.maxTentativasInvalidas(),
            principal));
  }
}

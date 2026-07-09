package br.com.softon.rbac.controller;

import br.com.softon.rbac.dto.response.DominioResponse;
import br.com.softon.rbac.dto.response.FuncionalidadeResponse;
import br.com.softon.rbac.dto.response.PermissaoResponse;
import br.com.softon.rbac.repository.DominioRepository;
import br.com.softon.rbac.repository.FuncionalidadeRepository;
import br.com.softon.rbac.repository.PermissaoRepository;
import br.com.softon.portal.shared.security.Permissoes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo RBAC read-only: expõe domínios, funcionalidades e permissões.
 * Mutations do catálogo (criar/alterar) são feitas via seeds Flyway —
 * não há endpoint de escrita porque o catálogo é o contrato do sistema.
 * Requer permissão {@code GRUPO_ACESSO:LER} (quem administra grupos
 * precisa visualizar o catálogo pra montar as permissões).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rbac/catalogo")
public class CatalogoController {

  private final DominioRepository dominioRepository;
  private final FuncionalidadeRepository funcionalidadeRepository;
  private final PermissaoRepository permissaoRepository;

  @GetMapping("/dominios")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public List<DominioResponse> dominios() {
    return dominioRepository.findAll().stream().map(DominioResponse::from).toList();
  }

  @GetMapping("/funcionalidades")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public List<FuncionalidadeResponse> funcionalidades() {
    return funcionalidadeRepository.findAll().stream().map(FuncionalidadeResponse::from).toList();
  }

  @GetMapping("/permissoes")
  @PreAuthorize(Permissoes.GRUPO_ACESSO_LER)
  public List<PermissaoResponse> permissoes() {
    return permissaoRepository.findAll().stream().map(PermissaoResponse::from).toList();
  }
}

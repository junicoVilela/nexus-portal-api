package com.nexus.identityaccess.service;

import com.nexus.identityaccess.repository.UsuarioRepository;
import com.nexus.portal.shared.security.AutoridadeResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RbacAutoridadeResolver implements AutoridadeResolver {

  private final UsuarioRepository usuarioRepository;
  private final RbacService rbacService;

  @Override
  @Transactional(readOnly = true)
  public List<String> permissoesDoUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    return usuarioRepository.findByUsernameAndAtivoTrue(username)
        .map(usuario -> rbacService.permissoesDoUsuario(usuario.getId()))
        .orElse(null);
  }
}

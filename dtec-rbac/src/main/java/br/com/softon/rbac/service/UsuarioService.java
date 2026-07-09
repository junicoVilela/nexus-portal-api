package br.com.softon.rbac.service;

import br.com.softon.rbac.dto.response.MeResponse;
import br.com.softon.rbac.entity.Usuario;
import br.com.softon.rbac.repository.UsuarioRepository;
import br.com.softon.rbac.service.RbacService;
import br.com.softon.portal.shared.config.JwtService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RbacService rbacService;
  private final AuditoriaService auditoriaService;

  public String autenticar(String username, String rawPassword) {
    Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username)
        .orElseThrow(() -> new BusinessException("Usuário ou senha inválidos."));
    if (!passwordEncoder.matches(rawPassword, usuario.getPassword())) {
      throw new BusinessException("Usuário ou senha inválidos.");
    }
    List<String> permissoes = rbacService.permissoesDoUsuario(usuario.getId());
    return jwtService.gerarToken(usuario.getUsername(), permissoes);
  }

  public MeResponse me(String username) {
    Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    return new MeResponse(
        usuario.getId(),
        usuario.getUsername(),
        usuario.getNome(),
        usuario.getEmail(),
        rbacService.gruposDoUsuario(usuario.getId()),
        rbacService.permissoesDoUsuario(usuario.getId()));
  }

  public List<Usuario> listar() {
    return usuarioRepository.findAll();
  }

  public Page<Usuario> listar(Pageable pageable) {
    return usuarioRepository.findAll(pageable);
  }

  public Usuario buscar(UUID id) {
    return usuarioRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
  }

  @Transactional
  public Usuario criar(String username, String rawPassword, String nome, String email, Principal principal) {
    if (usuarioRepository.existsByUsername(username)) {
      throw new BusinessException("Já existe usuário com esse nome.");
    }
    Usuario usuario = usuarioRepository.save(new Usuario(username, passwordEncoder.encode(rawPassword),
        nome, email));
    auditoriaService.registrar("Usuario", usuario.getId(), "CRIAR",
        "Usuário criado: " + usuario.getUsername(), principal);
    return usuario;
  }

  @Transactional
  public Usuario atualizar(UUID id, String nome, String email, boolean ativo, Principal principal) {
    Usuario usuario = buscar(id);
    usuario.atualizar(nome, email, ativo);
    auditoriaService.registrar("Usuario", id, ativo ? "EDITAR" : "DESATIVAR",
        "Usuário " + usuario.getUsername() + (ativo ? " atualizado." : " desativado."), principal);
    return usuario;
  }

  @Transactional
  public void alterarSenha(UUID id, String novaSenha, Principal principal) {
    Usuario usuario = buscar(id);
    usuario.alterarSenha(passwordEncoder.encode(novaSenha));
    auditoriaService.registrar("Usuario", id, "RESETAR_SENHA",
        "Senha do usuário " + usuario.getUsername() + " alterada.", principal);
  }
}

package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.response.MeResponse;
import br.com.softon.portal.docflow.entity.Usuario;
import br.com.softon.portal.docflow.repository.UsuarioRepository;
import br.com.softon.portal.shared.config.JwtService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
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

  public String autenticar(String username, String rawPassword) {
    Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username)
        .orElseThrow(() -> new BusinessException("Usuário ou senha inválidos."));
    if (!passwordEncoder.matches(rawPassword, usuario.getPassword())) {
      throw new BusinessException("Usuário ou senha inválidos.");
    }
    return jwtService.gerarToken(usuario.getUsername(), usuario.roleList());
  }

  public MeResponse me(String username) {
    Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    return new MeResponse(
        usuario.getId(),
        usuario.getUsername(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.roleList(),
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
  public Usuario criar(String username, String rawPassword, String nome, String email, String roles) {
    if (usuarioRepository.existsByUsername(username)) {
      throw new BusinessException("Já existe usuário com esse nome.");
    }
    return usuarioRepository.save(new Usuario(username, passwordEncoder.encode(rawPassword),
        nome, email, roles));
  }

  @Transactional
  public Usuario atualizar(UUID id, String nome, String email, String roles, boolean ativo) {
    Usuario usuario = buscar(id);
    usuario.atualizar(nome, email, roles, ativo);
    return usuario;
  }

  @Transactional
  public void alterarSenha(UUID id, String novaSenha) {
    Usuario usuario = buscar(id);
    usuario.alterarSenha(passwordEncoder.encode(novaSenha));
  }
}

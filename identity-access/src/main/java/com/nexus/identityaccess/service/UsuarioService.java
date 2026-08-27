package com.nexus.identityaccess.service;

import com.nexus.identityaccess.dto.response.MeResponse;
import com.nexus.identityaccess.entity.Usuario;
import com.nexus.identityaccess.repository.UsuarioRepository;
import com.nexus.identityaccess.service.RbacService;
import com.nexus.identityaccess.entity.Grupo;
import com.nexus.identityaccess.repository.GrupoRepository;
import com.nexus.portal.shared.config.JwtService;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  private final GrupoRepository grupoRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RbacService rbacService;
  private final AuditoriaService auditoriaService;
  private final HistoricoLoginService historicoLoginService;
  private final PoliticaSenhaService politicaSenhaService;
  private final SessaoService sessaoService;

  public String autenticar(String username, String rawPassword, String ipOrigem, String userAgent) {
    Usuario usuario = usuarioRepository.findByUsernameAndAtivoTrue(username).orElse(null);
    if (usuario == null) {
      historicoLoginService.registrar(null, username, ipOrigem, userAgent,
          false, "Usuário inexistente ou inativo.");
      throw new BusinessException("Usuário ou senha inválidos.");
    }
    if (usuario.isBloqueado()) {
      historicoLoginService.registrar(usuario.getId(), username, ipOrigem, userAgent,
          false, "Usuário bloqueado.");
      throw new BusinessException("Usuário bloqueado. Contate um administrador.");
    }
    if (!passwordEncoder.matches(rawPassword, usuario.getPassword())) {
      registrarFalhaESeNecessarioBloquear(usuario, username, ipOrigem, userAgent);
      throw new BusinessException("Usuário ou senha inválidos.");
    }
    usuario.setTentativasInvalidas(0);
    List<String> permissoes = rbacService.permissoesDoUsuario(usuario.getId());
    String jti = UUID.randomUUID().toString();
    String token = jwtService.gerarToken(usuario.getUsername(), permissoes, jti);
    OffsetDateTime expira = OffsetDateTime.now().plusNanos(jwtService.expirationMs() * 1_000_000L);
    sessaoService.abrir(jti, usuario.getId(), ipOrigem, userAgent, expira);
    historicoLoginService.registrar(usuario.getId(), username, ipOrigem, userAgent, true, null);
    return token;
  }

  private void registrarFalhaESeNecessarioBloquear(Usuario usuario, String username,
      String ipOrigem, String userAgent) {
    int novas = usuario.getTentativasInvalidas() + 1;
    usuario.setTentativasInvalidas(novas);
    int limite = politicaSenhaService.atual().getMaxTentativasInvalidas();
    String motivo = "Senha incorreta (" + novas + "/" + limite + ").";
    if (novas >= limite && !usuario.isBloqueado()) {
      usuario.bloquear();
      motivo += " Usuário bloqueado automaticamente.";
    }
    historicoLoginService.registrar(usuario.getId(), username, ipOrigem, userAgent, false, motivo);
  }

  /** Overload sem contexto de request (usado por testes e chamadas internas). */
  public String autenticar(String username, String rawPassword) {
    return autenticar(username, rawPassword, null, null);
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

  /**
   * E-mail de um usuário ativo, para outros módulos notificarem sem precisar do
   * repositório. Vazio quando o usuário não existe, está inativo ou não tem
   * e-mail cadastrado.
   */
  public Optional<String> emailDoUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    return usuarioRepository.findByUsernameAndAtivoTrue(username.trim())
        .map(Usuario::getEmail)
        .filter(email -> email != null && !email.isBlank());
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
    politicaSenhaService.validarOuFalhar(rawPassword);
    String hash = passwordEncoder.encode(rawPassword);
    Usuario usuario = usuarioRepository.save(new Usuario(username, hash, nome, email));
    politicaSenhaService.registrarNoHistorico(usuario.getId(), hash);
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
    politicaSenhaService.validarOuFalhar(novaSenha);
    if (politicaSenhaService.reutilizada(id, novaSenha)) {
      throw new BusinessException("Senha já usada recentemente. Escolha outra.");
    }
    String hash = passwordEncoder.encode(novaSenha);
    usuario.alterarSenha(hash);
    usuario.marcarTrocaSenhaProximoLogin(true);
    politicaSenhaService.registrarNoHistorico(id, hash);
    auditoriaService.registrar("Usuario", id, "RESETAR_SENHA",
        "Senha do usuário " + usuario.getUsername() + " alterada.", principal);
  }

  @Transactional
  public Usuario alterarBloqueio(UUID id, boolean bloquear, Principal principal) {
    Usuario usuario = buscar(id);
    if (bloquear) {
      usuario.bloquear();
    } else {
      usuario.desbloquear();
    }
    auditoriaService.registrar("Usuario", id, bloquear ? "BLOQUEAR" : "DESBLOQUEAR",
        "Usuário " + usuario.getUsername() + (bloquear ? " bloqueado." : " desbloqueado."),
        principal);
    return usuario;
  }

  public List<UUID> listarGrupos(UUID usuarioId) {
    buscar(usuarioId);
    return grupoRepository.findComUsuario(usuarioId).stream().map(Grupo::getId).toList();
  }

  /**
   * Substitui o conjunto de grupos do usuário pelos {@code grupoIds} informados.
   * Aplica a mudança pelos dois lados (adiciona nos grupos que ganharam o usuário,
   * remove dos que perderam) já que a coluna vive em tb_grupo_usuario e é a
   * entidade Grupo que a mantém como @ElementCollection.
   */
  @Transactional
  public List<UUID> salvarGrupos(UUID usuarioId, List<UUID> grupoIds, Principal principal) {
    Usuario usuario = buscar(usuarioId);
    Set<UUID> alvo = new HashSet<>(grupoIds == null ? List.of() : grupoIds);
    Set<UUID> atuais = new HashSet<>(grupoRepository.findComUsuario(usuarioId).stream()
        .map(Grupo::getId).toList());

    Set<UUID> paraAdicionar = new HashSet<>(alvo);
    paraAdicionar.removeAll(atuais);
    Set<UUID> paraRemover = new HashSet<>(atuais);
    paraRemover.removeAll(alvo);

    grupoRepository.findAllById(paraAdicionar).forEach(g -> {
      List<UUID> novaLista = new java.util.ArrayList<>(g.getUsuarios());
      if (!novaLista.contains(usuarioId)) {
        novaLista.add(usuarioId);
      }
      g.atualizarUsuarios(novaLista);
    });
    grupoRepository.findAllById(paraRemover).forEach(g -> {
      List<UUID> novaLista = new java.util.ArrayList<>(g.getUsuarios());
      novaLista.remove(usuarioId);
      g.atualizarUsuarios(novaLista);
    });

    auditoriaService.registrar("Usuario", usuarioId, "VINCULAR_GRUPOS",
        "Grupos do usuário " + usuario.getUsername() + " atualizados (" + alvo.size() + ").",
        principal);
    return alvo.stream().toList();
  }
}

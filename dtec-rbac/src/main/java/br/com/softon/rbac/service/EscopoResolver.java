package br.com.softon.rbac.service;

import br.com.softon.rbac.entity.EscopoAcesso;
import br.com.softon.rbac.entity.Grupo;
import br.com.softon.rbac.entity.Usuario;
import br.com.softon.rbac.repository.EscopoAcessoRepository;
import br.com.softon.rbac.repository.GrupoRepository;
import br.com.softon.rbac.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolve as restrições de escopo do usuário autenticado. É o ponto de
 * consulta pra qualquer módulo de negócio que queira aplicar filtro
 * automático baseado em escopo.
 *
 * <p><b>Semântica MVP:</b>
 * <ul>
 *   <li>Se o usuário não tem NENHUM escopo com clienteId → sem restrição.</li>
 *   <li>Se tem pelo menos um escopo com clienteId (direto ou via grupo) →
 *       whitelist da união desses clienteIds.</li>
 * </ul>
 * A flag {@code somente_leitura} ainda não é aplicada (backlog).
 */
@Service
@RequiredArgsConstructor
public class EscopoResolver {

  private final UsuarioRepository usuarioRepository;
  private final GrupoRepository grupoRepository;
  private final EscopoAcessoRepository escopoRepository;

  /**
   * Lista de clientes permitidos ao usuário. {@code Optional.empty()} = sem
   * restrição (enxerga tudo); {@code Optional.of(set)} = whitelist (mesmo
   * se o set estiver vazio — nesse caso não enxerga nenhum cliente).
   */
  public Optional<Set<UUID>> clientesPermitidos(UUID usuarioId) {
    if (usuarioId == null) return Optional.empty();
    Set<EscopoAcesso> escopos = coletar(usuarioId);
    if (escopos.isEmpty()) return Optional.empty();
    boolean algumTemCliente = escopos.stream().anyMatch(e -> e.getClienteId() != null);
    if (!algumTemCliente) return Optional.empty();
    Set<UUID> permitidos = new LinkedHashSet<>();
    for (EscopoAcesso e : escopos) {
      if (e.getClienteId() != null) permitidos.add(e.getClienteId());
    }
    return Optional.of(permitidos);
  }

  /**
   * Mesma semântica de {@link #clientesPermitidos(UUID)} mas resolvendo
   * o usuário atual via {@link SecurityContextHolder}. Retorna
   * {@code Optional.empty()} quando não há autenticação (usada em jobs
   * ou testes) — fail-open pra não bloquear caminhos internos.
   */
  public Optional<Set<UUID>> clientesPermitidosDoUsuarioAtual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) return Optional.empty();
    return usuarioRepository.findByUsernameAndAtivoTrue(auth.getName())
        .map(Usuario::getId)
        .flatMap(this::clientesPermitidos);
  }

  /** Verifica acesso a um cliente específico respeitando escopo. */
  public boolean podeAcessarCliente(UUID clienteId) {
    return clientesPermitidosDoUsuarioAtual()
        .map(permitidos -> permitidos.contains(clienteId))
        .orElse(true);
  }

  private Set<EscopoAcesso> coletar(UUID usuarioId) {
    Set<EscopoAcesso> out = new HashSet<>(escopoRepository.findByUsuarioIdAndAtivoTrue(usuarioId));
    // Grupos que o usuário pertence (ativos ou não, mas escopos ativos)
    List<Grupo> grupos = grupoRepository.findAtivosComUsuario(usuarioId);
    for (Grupo g : grupos) {
      out.addAll(escopoRepository.findByGrupoIdAndAtivoTrue(g.getId()));
    }
    return out;
  }
}

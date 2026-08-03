package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.request.ClienteRequest;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.ClienteModulo;
import com.nexus.portal.docflow.entity.ClientePagina;
import com.nexus.portal.docflow.entity.ClienteProjeto;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.repository.ClienteModuloRepository;
import com.nexus.portal.docflow.repository.ClientePaginaRepository;
import com.nexus.portal.docflow.repository.ClienteProjetoRepository;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.PreviewTokenRepository;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.identityaccess.service.AuditoriaService;
import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.util.SlugUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClienteService {

  private static final Pattern COR_HEX_PERMITIDO =
      Pattern.compile("#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})");

  private final ClienteRepository clienteRepository;
  private final ModuloService moduloService;
  private final PaginaService paginaService;
  private final ClienteModuloRepository clienteModuloRepository;
  private final ClientePaginaRepository clientePaginaRepository;
  private final ClienteProjetoRepository clienteProjetoRepository;
  private final ProjetoService projetoService;
  private final EscopoResolver escopoResolver;
  private final PublicacaoRepository publicacaoRepository;
  private final PreviewTokenRepository previewTokenRepository;
  private final AuditoriaService auditoriaService;
  private final ArquivoRemocaoService arquivoRemocaoService;

  @Transactional
  public Cliente criar(ClienteRequest request) {
    String slug = slugFrom(request.slug(), request.nome());
    if (clienteRepository.existsBySlug(slug)) {
      throw new BusinessException("Já existe cliente com o slug informado.");
    }
    Cliente cliente = new Cliente(request.nome().trim(), slug, active(request.ativo()));
    cliente.definirTemas(corTemaOuNull(request.temaCorPrimaria()), corTemaOuNull(request.temaCorFundo()));
    return clienteRepository.save(cliente);
  }

  @Transactional
  public Cliente atualizar(UUID id, ClienteRequest request) {
    Cliente cliente = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(id);
    String slug = slugFrom(request.slug(), request.nome());
    if (clienteRepository.existsBySlugAndIdNot(slug, id)) {
      throw new BusinessException("Já existe cliente com o slug informado.");
    }
    cliente.atualizar(request.nome().trim(), slug, active(request.ativo()),
        corTemaOuNull(request.temaCorPrimaria()), corTemaOuNull(request.temaCorFundo()));
    return cliente;
  }

  public Page<Cliente> listar(String nome, Pageable pageable) {
    String filtro = lowerBlankToNull(nome);
    Optional<Set<UUID>> permitidos = escopoResolver.clientesPermitidosDoUsuarioAtual();
    Specification<Cliente> specification = (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filtro != null) {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), "%" + filtro + "%"),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), "%" + filtro + "%")));
      }
      permitidos.ifPresent(ids -> {
        if (ids.isEmpty()) {
          // Whitelist vazia — usuário não vê nenhum cliente.
          predicates.add(criteriaBuilder.disjunction());
        } else {
          predicates.add(root.get("id").in(ids));
        }
      });
      return predicates.isEmpty()
          ? criteriaBuilder.conjunction()
          : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
    return clienteRepository.findAll(specification, pageable);
  }

  public List<Cliente> listar() {
    Optional<Set<UUID>> permitidos = escopoResolver.clientesPermitidosDoUsuarioAtual();
    List<Cliente> todos = clienteRepository.findAll(Sort.by("nome"));
    return permitidos.map(ids -> todos.stream().filter(c -> ids.contains(c.getId())).toList())
        .orElse(todos);
  }

  public Cliente buscar(UUID id) {
    Cliente cliente = clienteRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    if (!escopoResolver.podeAcessarCliente(id)) {
      // Semanticamente indistinguível de "não existe" — não vaza a existência.
      throw new NotFoundException("Cliente não encontrado.");
    }
    return cliente;
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Cliente cliente = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(id);
    if (publicacaoRepository.existsByCliente_Id(id)) {
      throw new BusinessException("Exclua primeiro as publicações deste cliente.");
    }

    Path logo = cliente.getLogoPath() == null || cliente.getLogoPath().isBlank()
        ? null
        : Path.of(cliente.getLogoPath());
    previewTokenRepository.deleteByClienteId(id);
    clienteRepository.delete(cliente);
    auditoriaService.registrar("CLIENTE", id, "EXCLUIR", "Cliente excluído: " + cliente.getNome(), principal);
    arquivoRemocaoService.removerAposCommit(logo);
  }

  @Transactional
  public void vincularProjetos(UUID clienteId, List<UUID> projetoIds) {
    Cliente cliente = buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    clienteProjetoRepository.deleteByCliente_Id(clienteId);
    if (projetoIds == null || projetoIds.isEmpty()) {
      return;
    }
    List<Projeto> projetos = projetoService.buscarTodos(projetoIds);
    clienteProjetoRepository.saveAll(projetos.stream()
        .map(projeto -> new ClienteProjeto(cliente, projeto))
        .toList());
  }

  @Transactional
  public void vincularModulos(UUID clienteId, List<UUID> moduloIds) {
    Cliente cliente = buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    clienteModuloRepository.deleteByCliente_Id(clienteId);
    if (moduloIds == null || moduloIds.isEmpty()) {
      return;
    }
    List<Modulo> modulos = moduloService.buscarTodos(moduloIds);
    clienteModuloRepository.saveAll(modulos.stream()
        .map(modulo -> new ClienteModulo(cliente, modulo))
        .toList());
  }

  @Transactional
  public void vincularPaginas(UUID clienteId, List<UUID> paginaIds) {
    Cliente cliente = buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    clientePaginaRepository.deleteByCliente_Id(clienteId);
    if (paginaIds == null || paginaIds.isEmpty()) {
      return;
    }
    List<Pagina> paginas = paginaService.buscarTodos(paginaIds);
    clientePaginaRepository.saveAll(paginas.stream()
        .map(pagina -> new ClientePagina(cliente, pagina))
        .toList());
  }

  public List<UUID> listarModuloIds(UUID clienteId) {
    buscar(clienteId);
    return clienteModuloRepository.findModuloIdsByClienteId(clienteId);
  }

  public List<UUID> listarProjetoIds(UUID clienteId) {
    buscar(clienteId);
    return clienteProjetoRepository.findProjetoIdsByClienteId(clienteId);
  }

  public List<UUID> listarPaginaIds(UUID clienteId) {
    buscar(clienteId);
    return clientePaginaRepository.findPaginaIdsByClienteId(clienteId);
  }

  @Transactional
  public void copiarVinculos(UUID origemClienteId, UUID destinoClienteId) {
    if (origemClienteId.equals(destinoClienteId)) {
      throw new BusinessException("Selecione clientes diferentes para copiar vínculos.");
    }
    buscar(origemClienteId);
    buscar(destinoClienteId);
    vincularProjetos(destinoClienteId, listarProjetoIds(origemClienteId));
    vincularModulos(destinoClienteId, listarModuloIds(origemClienteId));
    vincularPaginas(destinoClienteId, listarPaginaIds(origemClienteId));
  }

  private String slugFrom(String slug, String nome) {
    String normalized = SlugUtils.normalize(slug == null || slug.isBlank() ? nome : slug);
    if (normalized == null) {
      throw new BusinessException("Slug inválido.");
    }
    return normalized;
  }

  private boolean active(Boolean value) {
    return value == null || value;
  }

  private String corTemaOuNull(String cor) {
    if (cor == null || cor.isBlank()) {
      return null;
    }
    String t = cor.trim();
    if (!COR_HEX_PERMITIDO.matcher(t).matches()) {
      throw new BusinessException("Cor de tema inválida. Use formato hexadecimal (#RGB, #RRGGBB ou #RRGGBBAA).");
    }
    return t;
  }

  private String lowerBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase();
  }
}

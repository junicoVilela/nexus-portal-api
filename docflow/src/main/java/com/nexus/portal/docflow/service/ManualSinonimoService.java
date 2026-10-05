package com.nexus.portal.docflow.service;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.docflow.entity.ManualSinonimo;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ManualSinonimoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sinônimos da busca do manual por cliente. Valem na hora para perguntas, MCP e manual hospedado
 * ({@code sinonimos.json} servido do banco); o ZIP leva a cópia da publicação.
 */
@Service
public class ManualSinonimoService {

  static final int MAX_TERMOS = 10;
  static final int MAX_CHARS_TERMO = 60;

  private final ManualSinonimoRepository repository;
  private final ClienteRepository clienteRepository;
  private final EscopoResolver escopoResolver;

  public ManualSinonimoService(ManualSinonimoRepository repository, ClienteRepository clienteRepository,
      EscopoResolver escopoResolver) {
    this.repository = repository;
    this.clienteRepository = clienteRepository;
    this.escopoResolver = escopoResolver;
  }

  @Transactional(readOnly = true)
  public List<ManualSinonimo> listar(UUID clienteId) {
    if (!escopoResolver.podeAcessarCliente(clienteId)) {
      throw new NotFoundException("Cliente não encontrado.");
    }
    return repository.findByClienteIdOrderByCreatedAtAsc(clienteId);
  }

  @Transactional
  public ManualSinonimo criar(UUID clienteId, List<String> termos, Principal principal) {
    clienteRepository.findById(clienteId).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    List<String> validos = validar(clienteId, null, termos);
    return repository.save(new ManualSinonimo(clienteId, validos, principal == null ? null : principal.getName()));
  }

  @Transactional
  public ManualSinonimo atualizar(UUID id, List<String> termos) {
    ManualSinonimo sinonimo = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(sinonimo.getClienteId());
    sinonimo.atualizar(validar(sinonimo.getClienteId(), id, termos));
    return sinonimo;
  }

  @Transactional
  public void excluir(UUID id) {
    ManualSinonimo sinonimo = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(sinonimo.getClienteId());
    repository.delete(sinonimo);
  }

  /** Grupos já normalizados para a busca (sem escopo: uso interno do leitor e do gerador). */
  @Transactional(readOnly = true)
  public List<List<String>> grupos(UUID clienteId) {
    if (clienteId == null) {
      return List.of();
    }
    return repository.findByClienteIdOrderByCreatedAtAsc(clienteId).stream()
        .map(s -> s.getTermos().stream().map(ManualSinonimos::normalizar).filter(t -> !t.isEmpty()).distinct().toList())
        .filter(grupo -> grupo.size() > 1)
        .toList();
  }

  private ManualSinonimo buscar(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Grupo de sinônimos não encontrado."));
  }

  /**
   * Pelo menos dois termos diferentes; cada termo em um só grupo do cliente, senão a busca não
   * saberia qual troca fazer.
   */
  private List<String> validar(UUID clienteId, UUID proprioId, List<String> termos) {
    Map<String, String> porNormalizado = new LinkedHashMap<>();
    for (String termo : termos == null ? List.<String>of() : termos) {
      String limpo = termo == null ? "" : termo.strip().replaceAll("\\s+", " ");
      String normalizado = ManualSinonimos.normalizar(limpo);
      if (normalizado.isEmpty()) {
        continue;
      }
      if (limpo.length() > MAX_CHARS_TERMO) {
        throw new BusinessException("Cada termo pode ter até " + MAX_CHARS_TERMO + " caracteres.");
      }
      porNormalizado.putIfAbsent(normalizado, limpo);
    }
    if (porNormalizado.size() < 2) {
      throw new BusinessException("Informe pelo menos dois termos diferentes (ex.: nota fiscal, NF).");
    }
    if (porNormalizado.size() > MAX_TERMOS) {
      throw new BusinessException("Um grupo pode ter até " + MAX_TERMOS + " termos.");
    }
    for (ManualSinonimo outro : repository.findByClienteIdOrderByCreatedAtAsc(clienteId)) {
      if (outro.getId().equals(proprioId)) {
        continue;
      }
      for (String termo : outro.getTermos()) {
        String repetido = porNormalizado.get(ManualSinonimos.normalizar(termo));
        if (repetido != null) {
          throw new BusinessException("\"" + repetido + "\" já está no grupo " + String.join(", ", outro.getTermos())
              + ". Edite aquele grupo.");
        }
      }
    }
    return new ArrayList<>(porNormalizado.values());
  }
}

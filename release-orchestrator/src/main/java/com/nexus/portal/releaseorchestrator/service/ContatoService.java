package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ContatoRequest;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Contato;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorContatoRepository;
import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("orchestratorContatoService")
@RequiredArgsConstructor
public class ContatoService {

  private final OrchestratorContatoRepository repository;
  private final ClienteService clienteService;
  private final EscopoResolver escopoResolver;

  public List<Contato> listar(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_IdOrderByNomeAsc(clienteId);
  }

  public Contato buscar(UUID clienteId, UUID id) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_IdAndId(clienteId, id)
        .orElseThrow(() -> new NotFoundException("Contato não encontrado para o cliente."));
  }

  @Transactional
  public Contato criar(UUID clienteId, ContatoRequest request) {
    Cliente cliente = clienteService.buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    return repository.save(new Contato(
        cliente, request.nome(), request.papel(), request.email(), request.telefone()));
  }

  @Transactional
  public Contato atualizar(UUID clienteId, UUID id, ContatoRequest request) {
    Contato contato = buscar(clienteId, id);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    contato.atualizar(request.nome(), request.papel(), request.email(), request.telefone());
    return contato;
  }

  @Transactional
  public void excluir(UUID clienteId, UUID id) {
    Contato contato = buscar(clienteId, id);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    repository.delete(contato);
  }
}

package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.ContatoRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.Contato;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorContatoRepository;
import br.com.softon.portal.shared.exception.NotFoundException;
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

  public List<Contato> listar(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_IdOrderByNomeAsc(clienteId);
  }

  public Contato buscar(UUID clienteId, UUID id) {
    return repository.findByCliente_IdAndId(clienteId, id)
        .orElseThrow(() -> new NotFoundException("Contato não encontrado para o cliente."));
  }

  @Transactional
  public Contato criar(UUID clienteId, ContatoRequest request) {
    Cliente cliente = clienteService.buscar(clienteId);
    return repository.save(new Contato(
        cliente, request.nome(), request.papel(), request.email(), request.telefone()));
  }

  @Transactional
  public Contato atualizar(UUID clienteId, UUID id, ContatoRequest request) {
    Contato contato = buscar(clienteId, id);
    contato.atualizar(request.nome(), request.papel(), request.email(), request.telefone());
    return contato;
  }

  @Transactional
  public void excluir(UUID clienteId, UUID id) {
    Contato contato = buscar(clienteId, id);
    repository.delete(contato);
  }
}

package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.ConfigEntregaRequest;
import br.com.softon.portal.releaseorchestrator.entity.Cliente;
import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorConfigEntregaRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Configuração de entrega 1:1 com cliente. MVP: só destino PASTA permitido
 * (FTP/SFTP/BUCKET ainda exigirão credenciais — pós-MVP).
 */
@Service("orchestratorConfigEntregaService")
@RequiredArgsConstructor
public class ConfigEntregaService {

  private final OrchestratorConfigEntregaRepository repository;
  private final ClienteService clienteService;

  public ConfigEntrega buscar(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_Id(clienteId)
        .orElseThrow(() -> new NotFoundException("Configuração de entrega ainda não definida para este cliente."));
  }

  /**
   * Upsert da configuração. Se ainda não existe, cria; caso contrário,
   * atualiza in-place (Hibernate dirty checking).
   */
  @Transactional
  public ConfigEntrega salvar(UUID clienteId, ConfigEntregaRequest request) {
    validar(request);
    Cliente cliente = clienteService.buscar(clienteId);
    boolean exigirAprovacao = request.exigirAprovacao() != null && request.exigirAprovacao();

    return repository.findByCliente_Id(clienteId)
        .map(existente -> {
          existente.atualizar(
              request.tipoDestino(), request.caminhoBase(),
              exigirAprovacao, request.emailsNotificacao());
          return existente;
        })
        .orElseGet(() -> {
          ConfigEntrega novo = new ConfigEntrega(cliente, request.tipoDestino(),
              request.caminhoBase());
          novo.atualizar(request.tipoDestino(), request.caminhoBase(),
              exigirAprovacao, request.emailsNotificacao());
          return repository.save(novo);
        });
  }

  private void validar(ConfigEntregaRequest request) {
    if (request.tipoDestino() != TipoDestinoEntrega.PASTA) {
      throw new BusinessException(
          "Apenas destino PASTA é suportado no MVP. " + request.tipoDestino()
              + " entra em fase pós-MVP.");
    }
    String caminho = request.caminhoBase();
    if (caminho == null || caminho.isBlank()) {
      throw new BusinessException("Caminho base é obrigatório para destino PASTA.");
    }
    if (caminho.contains("..")) {
      throw new BusinessException("Caminho base não pode conter '..' (path traversal).");
    }
    if (!caminho.startsWith("/")) {
      throw new BusinessException("Caminho base precisa ser absoluto (começar com '/').");
    }
  }
}

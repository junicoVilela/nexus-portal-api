package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ConfigEntregaRequest;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import com.nexus.portal.releaseorchestrator.integration.publish.PublishException;
import com.nexus.portal.releaseorchestrator.integration.publish.PublishService;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorConfigEntregaRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Configuração de entrega 1:1 com cliente. F3 fase 1: aceita PASTA, FTP e
 * SFTP no cadastro (cifragem de senha em AES-GCM). Transferência real
 * para FTP/SFTP vem em fase posterior — por enquanto os stubs validam
 * configuração e retornam mensagem amigável.
 */
@Service("orchestratorConfigEntregaService")
@RequiredArgsConstructor
public class ConfigEntregaService {

  private final OrchestratorConfigEntregaRepository repository;
  private final ClienteService clienteService;
  private final EncryptionService encryptionService;
  private final PublishService publishService;

  public ConfigEntrega buscar(UUID clienteId) {
    clienteService.buscar(clienteId);
    return repository.findByCliente_Id(clienteId)
        .orElseThrow(() -> new NotFoundException(
            "Configuração de entrega ainda não definida para este cliente."));
  }

  /**
   * Upsert da configuração. Se ainda não existe, cria; caso contrário,
   * atualiza in-place (Hibernate dirty checking). Senha em texto é cifrada
   * antes de persistir; campo vazio no PUT preserva a senha atual.
   */
  @Transactional
  public ConfigEntrega salvar(UUID clienteId, ConfigEntregaRequest request) {
    validar(request);
    Cliente cliente = clienteService.buscar(clienteId);
    boolean exigirAprovacao = request.exigirAprovacao() != null && request.exigirAprovacao();
    String senhaCifrada = (request.senha() != null && !request.senha().isBlank())
        ? encryptionService.cifrar(request.senha())
        : null; // null = preserva a atual em atualizarDestinoRemoto

    return repository.findByCliente_Id(clienteId)
        .map(existente -> {
          aplicar(existente, request, exigirAprovacao, senhaCifrada);
          return existente;
        })
        .orElseGet(() -> {
          ConfigEntrega novo = new ConfigEntrega(cliente, request.tipoDestino(),
              request.caminhoBase());
          aplicar(novo, request, exigirAprovacao, senhaCifrada);
          return repository.save(novo);
        });
  }

  private void aplicar(ConfigEntrega cfg, ConfigEntregaRequest request,
      boolean exigirAprovacao, String senhaCifrada) {
    cfg.atualizar(request.tipoDestino(), request.caminhoBase(),
        exigirAprovacao, request.emailsNotificacao());
    if (request.tipoDestino() == TipoDestinoEntrega.BUCKET) {
      cfg.atualizarDestinoBucket(request.bucket(), request.endpoint(),
          request.regiao(), request.pathStyleAccess(),
          request.usuario(), senhaCifrada);
    } else {
      cfg.atualizarDestinoRemoto(request.host(), request.porta(),
          request.usuario(), senhaCifrada,
          request.modoPassivo(), request.strictHostCheck());
    }
  }

  /**
   * Testa a conexão com o destino configurado, sem persistir nada. Útil
   * para o operador validar antes de salvar.
   *
   * @return mensagem amigável; lança {@link BusinessException} em erro.
   */
  @Transactional
  public String testarConexao(UUID clienteId) {
    ConfigEntrega config = buscar(clienteId);
    try {
      publishService.testarConexao(config);
      return "Conexão OK para destino " + config.getTipoDestino() + ".";
    } catch (PublishException e) {
      throw new BusinessException(e.getMessage());
    }
  }

  private void validar(ConfigEntregaRequest request) {
    TipoDestinoEntrega tipo = request.tipoDestino();
    if (tipo == TipoDestinoEntrega.PASTA) {
      validarPasta(request);
    } else if (tipo == TipoDestinoEntrega.FTP || tipo == TipoDestinoEntrega.SFTP) {
      validarRemoto(request);
    } else if (tipo == TipoDestinoEntrega.BUCKET) {
      validarBucket(request);
    } else {
      throw new BusinessException(
          "Destino " + tipo + " ainda não suportado (fase F3 pendente).");
    }
  }

  private void validarBucket(ConfigEntregaRequest request) {
    if (request.bucket() == null || request.bucket().isBlank()) {
      throw new BusinessException("Bucket obrigatório para destino BUCKET.");
    }
    if (request.usuario() == null || request.usuario().isBlank()) {
      throw new BusinessException("Access key (usuário) obrigatória para destino BUCKET.");
    }
    // Secret key vazia é permitida quando já há senha cadastrada (preserva).
  }

  private void validarPasta(ConfigEntregaRequest request) {
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

  private void validarRemoto(ConfigEntregaRequest request) {
    if (request.host() == null || request.host().isBlank()) {
      throw new BusinessException("Host obrigatório para destino " + request.tipoDestino() + ".");
    }
    if (request.usuario() == null || request.usuario().isBlank()) {
      throw new BusinessException("Usuário obrigatório para destino " + request.tipoDestino() + ".");
    }
    // Senha vazia é permitida quando já há senha cadastrada (preserva); service
    // que chama salvar() não consegue saber sem buscar — confiamos no PUT.
    if (request.caminhoBase() == null || request.caminhoBase().isBlank()) {
      throw new BusinessException(
          "Caminho base obrigatório (pasta destino no servidor remoto).");
    }
  }
}

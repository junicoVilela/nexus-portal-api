package com.nexus.identityaccess.service;

import com.nexus.identityaccess.entity.EscopoAcesso;
import com.nexus.identityaccess.entity.EscopoAcesso.TipoAmbiente;
import com.nexus.identityaccess.repository.EscopoAcessoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD dos escopos. A APLICAÇÃO das restrições (filtro por cliente/ambiente/
 * produto nas queries) é backlog — os módulos de negócio ainda enxergam
 * dados sem filtrar por escopo.
 */
@Service
@RequiredArgsConstructor
public class EscopoAcessoService {

  private final EscopoAcessoRepository repository;
  private final AuditoriaService auditoriaService;

  public List<EscopoAcesso> listarTodos() {
    return repository.findAll();
  }

  public List<EscopoAcesso> listarPorUsuario(UUID usuarioId) {
    return repository.findByUsuarioIdAndAtivoTrue(usuarioId);
  }

  public List<EscopoAcesso> listarPorGrupo(UUID grupoId) {
    return repository.findByGrupoIdAndAtivoTrue(grupoId);
  }

  public EscopoAcesso buscar(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Escopo não encontrado."));
  }

  @Transactional
  public EscopoAcesso criar(UUID usuarioId, UUID grupoId, UUID clienteId, UUID ambienteId,
      UUID produtoId, String tipoAmbienteRaw, boolean somenteLeitura, boolean ativo,
      Principal principal) {
    if (usuarioId == null && grupoId == null) {
      throw new BusinessException("Informe usuário ou grupo.");
    }
    EscopoAcesso e = repository.save(new EscopoAcesso(usuarioId, grupoId, clienteId, ambienteId,
        produtoId, tipoAmbiente(tipoAmbienteRaw), somenteLeitura, ativo));
    auditoriaService.registrar("EscopoAcesso", e.getId(), "CRIAR",
        "Escopo criado.", principal);
    return e;
  }

  @Transactional
  public EscopoAcesso atualizar(UUID id, UUID clienteId, UUID ambienteId, UUID produtoId,
      String tipoAmbienteRaw, boolean somenteLeitura, boolean ativo, Principal principal) {
    EscopoAcesso e = buscar(id);
    e.atualizar(clienteId, ambienteId, produtoId, tipoAmbiente(tipoAmbienteRaw),
        somenteLeitura, ativo);
    auditoriaService.registrar("EscopoAcesso", id, "EDITAR", "Escopo atualizado.", principal);
    return e;
  }

  @Transactional
  public EscopoAcesso alterarStatus(UUID id, boolean ativo, Principal principal) {
    EscopoAcesso e = buscar(id);
    e.setAtivo(ativo);
    auditoriaService.registrar("EscopoAcesso", id, ativo ? "ATIVAR" : "DESATIVAR",
        "Escopo " + (ativo ? "ativado." : "desativado."), principal);
    return e;
  }

  @Transactional
  public void remover(UUID id, Principal principal) {
    EscopoAcesso e = buscar(id);
    repository.delete(e);
    auditoriaService.registrar("EscopoAcesso", id, "EXCLUIR", "Escopo removido.", principal);
  }

  private TipoAmbiente tipoAmbiente(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return TipoAmbiente.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("Tipo de ambiente inválido: " + raw);
    }
  }
}

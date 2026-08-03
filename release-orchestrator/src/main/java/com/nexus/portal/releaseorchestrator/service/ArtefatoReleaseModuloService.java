package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.repository.ArtefatoReleaseModuloRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.releaseorchestrator.service.ArtefatoStorageService.ArtefatoStored;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload, listagem, download e exclusão de artefatos por release+módulo (F0.6).
 *
 * Regras (spec §3, §7.6, §7.9):
 * <ul>
 *   <li>Tipos {@code FUNCIONALIDADES} e {@code REGRAS} não aceitam upload.</li>
 *   <li>Extensão validada contra {@link TipoModulo#extensoesAceitasDefault()}.</li>
 *   <li>Releases {@code PUBLICADA} ou {@code CANCELADA} ficam imutáveis —
 *       nem upload nem exclusão.</li>
 *   <li>SHA-256 e tamanho são calculados pelo {@link ArtefatoStorageService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ArtefatoReleaseModuloService {

  private final ArtefatoReleaseModuloRepository repository;
  private final ReleaseRepository releaseRepository;
  private final ModuloProdutoRepository moduloRepository;
  private final ArtefatoStorageService storage;

  public List<ArtefatoReleaseModulo> listar(UUID releaseId, UUID moduloId) {
    requerirReleaseEModulo(releaseId, moduloId);
    return repository.findByRelease_IdAndModuloProduto_IdOrderByCreatedAtDesc(releaseId, moduloId);
  }

  public ArtefatoReleaseModulo buscar(UUID releaseId, UUID moduloId, UUID id) {
    return repository.findByRelease_IdAndModuloProduto_IdAndId(releaseId, moduloId, id)
        .orElseThrow(() -> new NotFoundException("Artefato não encontrado."));
  }

  public Resource download(UUID releaseId, UUID moduloId, UUID id) {
    ArtefatoReleaseModulo artefato = buscar(releaseId, moduloId, id);
    return storage.arquivo(artefato.getCaminhoArmazenado());
  }

  @Transactional
  public ArtefatoReleaseModulo upload(UUID releaseId, UUID moduloId, MultipartFile file,
      String observacao) {
    ReleaseEModulo ctx = requerirReleaseEModulo(releaseId, moduloId);
    bloquearSeReleaseImutavel(ctx.release());
    validarUpload(file, ctx.modulo().getTipo());

    String nomeOriginal = nomeOriginalSeguro(file);
    ArtefatoStored stored;
    try {
      stored = storage.armazenar(releaseId, moduloId, nomeOriginal, file.getInputStream());
    } catch (IOException ex) {
      throw new BusinessException("Falha ao ler upload: " + ex.getMessage());
    }

    ArtefatoReleaseModulo artefato = new ArtefatoReleaseModulo(
        ctx.release(), ctx.modulo(), nomeOriginal,
        stored.caminho(), stored.sha256(), stored.tamanhoBytes(), observacao);
    return repository.save(artefato);
  }

  @Transactional
  public void excluir(UUID releaseId, UUID moduloId, UUID id) {
    ArtefatoReleaseModulo artefato = buscar(releaseId, moduloId, id);
    bloquearSeReleaseImutavel(artefato.getRelease());
    storage.remover(artefato.getCaminhoArmazenado());
    repository.delete(artefato);
  }

  private void validarUpload(MultipartFile file, TipoModulo tipo) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Arquivo de upload vazio ou ausente.");
    }
    if (!tipo.aceitaUploadDeArtefato()) {
      throw new BusinessException(
          "Tipo de módulo " + tipo + " não aceita upload de artefato.");
    }
    String nome = nomeOriginalSeguro(file);
    if (!tipo.aceitaExtensao(nome)) {
      throw new BusinessException(
          "Extensão não permitida para módulo " + tipo + ". Aceitas: "
              + tipo.extensoesAceitasDefault());
    }
  }

  private void bloquearSeReleaseImutavel(Release release) {
    ReleaseStatus s = release.getStatus();
    if (s == ReleaseStatus.PUBLICADA || s == ReleaseStatus.CANCELADA) {
      throw new BusinessException(
          "Release " + s + " é imutável — artefatos não podem ser alterados.");
    }
  }

  private ReleaseEModulo requerirReleaseEModulo(UUID releaseId, UUID moduloId) {
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    ModuloProduto modulo = moduloRepository.findById(moduloId)
        .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
    if (!modulo.getProduto().getId().equals(release.getProduto().getId())) {
      throw new BusinessException(
          "Módulo informado não pertence ao produto da release.");
    }
    return new ReleaseEModulo(release, modulo);
  }

  private String nomeOriginalSeguro(MultipartFile file) {
    String nome = file.getOriginalFilename();
    return (nome == null || nome.isBlank()) ? "artefato" : nome;
  }

  private record ReleaseEModulo(Release release, ModuloProduto modulo) {}
}

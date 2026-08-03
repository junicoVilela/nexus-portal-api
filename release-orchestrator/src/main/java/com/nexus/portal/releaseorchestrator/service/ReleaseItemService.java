package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.ReleaseItemRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ReordenarItensRequest;
import com.nexus.portal.releaseorchestrator.entity.AcaoHistorico;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseHistorico;
import com.nexus.portal.releaseorchestrator.entity.ReleaseItem;
import com.nexus.portal.releaseorchestrator.entity.VisibilidadeItem;
import com.nexus.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseItemRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ReleaseItemService {

    private final ReleaseItemRepository itemRepository;
    private final ReleaseHistoricoRepository historicoRepository;
    private final ReleaseService releaseService;

    public List<ReleaseItem> listar(UUID releaseId) {
        releaseService.buscar(releaseId);
        return itemRepository.findByReleaseIdOrderByOrdemAsc(releaseId);
    }

    @Transactional
    public ReleaseItem adicionar(UUID releaseId, ReleaseItemRequest request) {
        Release release = releaseService.buscar(releaseId);
        validarEdicao(release);
        int ordem = request.ordem() != null ? request.ordem()
                : itemRepository.findMaxOrdemByReleaseId(releaseId) + 1;
        VisibilidadeItem visibilidade = request.visibilidade() != null
                ? request.visibilidade() : VisibilidadeItem.TODOS;
        ReleaseItem item = new ReleaseItem(release, request.categoria(), request.titulo(),
                request.descricao(), visibilidade, ordem,
                request.ticket(), request.commit(), request.pullRequest(), request.responsavelId());
        item = itemRepository.save(item);
        registrarHistorico(release, AcaoHistorico.ITEM_ADICIONADO, "Item adicionado: " + request.titulo());
        return item;
    }

    @Transactional
    public ReleaseItem atualizar(UUID releaseId, UUID itemId, ReleaseItemRequest request) {
        Release release = releaseService.buscar(releaseId);
        validarEdicao(release);
        ReleaseItem item = buscarItem(itemId, releaseId);
        item.atualizar(request.categoria(), request.titulo(), request.descricao(),
                request.visibilidade(), request.ticket(),
                request.commit(), request.pullRequest(), request.responsavelId());
        registrarHistorico(release, AcaoHistorico.ITEM_EDITADO, "Item editado: " + request.titulo());
        return item;
    }

    @Transactional
    public void remover(UUID releaseId, UUID itemId) {
        Release release = releaseService.buscar(releaseId);
        validarEdicao(release);
        ReleaseItem item = buscarItem(itemId, releaseId);
        String titulo = item.getTitulo();
        itemRepository.delete(item);
        registrarHistorico(release, AcaoHistorico.ITEM_REMOVIDO, "Item removido: " + titulo);
    }

    @Transactional
    public void reordenar(UUID releaseId, ReordenarItensRequest request) {
        Release release = releaseService.buscar(releaseId);
        validarEdicao(release);
        Map<UUID, Integer> novasOrdens = request.ordens().stream()
                .collect(Collectors.toMap(
                        ReordenarItensRequest.ItemOrdem::id,
                        ReordenarItensRequest.ItemOrdem::ordem));
        List<ReleaseItem> itens = itemRepository.findByReleaseIdOrderByOrdemAsc(releaseId);
        for (ReleaseItem item : itens) {
            if (novasOrdens.containsKey(item.getId())) {
                item.setOrdem(novasOrdens.get(item.getId()));
            }
        }
    }

    @Transactional
    public ReleaseItem duplicar(UUID releaseId, UUID itemId) {
        Release release = releaseService.buscar(releaseId);
        validarEdicao(release);
        ReleaseItem original = buscarItem(itemId, releaseId);
        int novaOrdem = itemRepository.findMaxOrdemByReleaseId(releaseId) + 1;
        ReleaseItem copia = new ReleaseItem(release, original.getCategoria(),
                "[Cópia] " + original.getTitulo(), original.getDescricao(),
                original.getVisibilidade(), novaOrdem,
                original.getTicket(), original.getCommit(), original.getPullRequest(),
                original.getResponsavelId());
        return itemRepository.save(copia);
    }

    private ReleaseItem buscarItem(UUID itemId, UUID releaseId) {
        return itemRepository.findById(itemId)
                .filter(i -> i.getRelease().getId().equals(releaseId))
                .orElseThrow(() -> new NotFoundException("Item não encontrado."));
    }

    private void validarEdicao(Release release) {
        if (!release.podeEditar()) {
            throw new BusinessException(
                    "Não é possível modificar itens de uma release com status: " + release.getStatus());
        }
    }

    private void registrarHistorico(Release release, AcaoHistorico acao, String descricao) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth == null ? "system" : auth.getName();
        historicoRepository.save(new ReleaseHistorico(
                release.getId(), acao, descricao, null, null, usuario));
    }
}

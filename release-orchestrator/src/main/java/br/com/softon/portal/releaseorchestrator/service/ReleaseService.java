package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusReleaseRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ReleaseRequest;
import br.com.softon.portal.releaseorchestrator.dto.response.RevisaoValidacaoResponse;
import br.com.softon.portal.releaseorchestrator.entity.AcaoHistorico;
import br.com.softon.portal.releaseorchestrator.entity.CategoriaItem;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.entity.Release;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseHistorico;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseItem;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.VisibilidadeItem;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseItemRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    // Grafo de transições de status permitidas
    private static final Map<ReleaseStatus, Set<ReleaseStatus>> TRANSICOES_PERMITIDAS = Map.of(
            ReleaseStatus.RASCUNHO,           Set.of(ReleaseStatus.EM_DESENVOLVIMENTO, ReleaseStatus.CANCELADA),
            ReleaseStatus.EM_DESENVOLVIMENTO,  Set.of(ReleaseStatus.EM_REVISAO, ReleaseStatus.CANCELADA),
            ReleaseStatus.EM_REVISAO,          Set.of(ReleaseStatus.APROVADA, ReleaseStatus.RASCUNHO, ReleaseStatus.CANCELADA),
            ReleaseStatus.APROVADA,            Set.of(ReleaseStatus.PUBLICADA, ReleaseStatus.EM_REVISAO, ReleaseStatus.CANCELADA),
            ReleaseStatus.PUBLICADA,           Set.of(),
            ReleaseStatus.CANCELADA,           Set.of()
    );

    private final ReleaseRepository releaseRepository;
    private final ReleaseItemRepository itemRepository;
    private final ReleaseHistoricoRepository historicoRepository;
    private final ProdutoRhService produtoService;

    @Transactional
    public Release criar(ReleaseRequest request) {
        ProdutoRh produto = produtoService.buscar(request.produtoId());
        if (!produto.isAtivo()) {
            throw new BusinessException("Não é possível criar release para produto inativo.");
        }
        if (releaseRepository.existsByProdutoIdAndVersao(produto.getId(), request.versao().trim())) {
            throw new BusinessException("Já existe uma release com esta versão para o produto informado.");
        }
        ReleaseStatus status = request.status() != null ? request.status() : ReleaseStatus.RASCUNHO;
        Release release = new Release(produto, request.versao(), request.titulo(),
                request.tipo(), status, request.dataPrevista(),
                request.responsavelId(), request.resumo(), request.observacoes());
        release = releaseRepository.save(release);
        registrarHistorico(release, AcaoHistorico.CRIADA, "Release criada", null, status);
        return release;
    }

    @Transactional
    public Release atualizar(UUID id, ReleaseRequest request) {
        Release release = buscar(id);
        if (!release.podeEditar()) {
            throw new BusinessException("Release não pode ser editada no status atual: " + release.getStatus());
        }
        ProdutoRh produto = produtoService.buscar(request.produtoId());
        if (releaseRepository.existsByProdutoIdAndVersaoAndIdNot(produto.getId(), request.versao().trim(), id)) {
            throw new BusinessException("Já existe uma release com esta versão para o produto informado.");
        }
        release.atualizar(produto, request.versao(), request.titulo(), request.tipo(),
                request.dataPrevista(), request.responsavelId(),
                request.resumo(), request.observacoes());
        registrarHistorico(release, AcaoHistorico.EDITADA, "Release atualizada", null, null);
        return release;
    }

    @Transactional
    public Release alterarStatus(UUID id, AlterarStatusReleaseRequest request) {
        Release release = buscar(id);
        ReleaseStatus statusAtual = release.getStatus();
        ReleaseStatus novoStatus = request.status();

        Set<ReleaseStatus> permitidos = TRANSICOES_PERMITIDAS.getOrDefault(statusAtual, Set.of());
        if (!permitidos.contains(novoStatus)) {
            throw new BusinessException(
                    "Transição de status inválida: " + statusAtual + " → " + novoStatus);
        }

        release.alterarStatus(novoStatus);
        AcaoHistorico acao = resolverAcao(novoStatus);
        String descricao = request.observacao() != null ? request.observacao() : "Status alterado para " + novoStatus;
        registrarHistorico(release, acao, descricao, statusAtual, novoStatus);
        return release;
    }

    @Transactional
    public Release publicar(UUID id) {
        Release release = buscar(id);
        if (release.getStatus() != ReleaseStatus.APROVADA) {
            throw new BusinessException("Somente releases com status APROVADA podem ser publicadas.");
        }
        RevisaoValidacaoResponse validacao = validar(id);
        if (!validacao.valida()) {
            throw new BusinessException("Release não está pronta para publicação: " + validacao.pendencias());
        }
        ReleaseStatus statusAnterior = release.getStatus();
        release.publicar(username());
        registrarHistorico(release, AcaoHistorico.PUBLICADA, "Release publicada", statusAnterior, ReleaseStatus.PUBLICADA);
        return release;
    }

    @Transactional
    public Release cancelar(UUID id, String motivo) {
        Release release = buscar(id);
        if (release.getStatus() == ReleaseStatus.PUBLICADA || release.getStatus() == ReleaseStatus.CANCELADA) {
            throw new BusinessException("Release não pode ser cancelada no status atual: " + release.getStatus());
        }
        ReleaseStatus statusAnterior = release.getStatus();
        release.cancelar();
        String descricao = motivo != null && !motivo.isBlank() ? motivo : "Release cancelada";
        registrarHistorico(release, AcaoHistorico.CANCELADA, descricao, statusAnterior, ReleaseStatus.CANCELADA);
        return release;
    }

    @Transactional
    public Release duplicar(UUID id) {
        Release original = buscar(id);
        List<ReleaseItem> itensOriginais = itemRepository.findByReleaseIdOrderByOrdemAsc(id);

        String novaVersao = original.getVersao() + "-copia";
        Release copia = new Release(
                original.getProduto(), novaVersao,
                "[Cópia] " + original.getTitulo(),
                original.getTipo(), ReleaseStatus.RASCUNHO,
                null, original.getResponsavelId(),
                original.getResumo(), null);
        copia = releaseRepository.save(copia);

        for (ReleaseItem item : itensOriginais) {
            ReleaseItem cópia = new ReleaseItem(copia, item.getCategoria(), item.getTitulo(),
                    item.getDescricao(), item.getVisibilidade(), item.getOrdem(),
                    item.getTicket(), item.getCommit(), item.getPullRequest(), item.getResponsavelId());
            itemRepository.save(cópia);
        }

        registrarHistorico(copia, AcaoHistorico.DUPLICADA,
                "Duplicada a partir da release " + original.getVersao(), null, ReleaseStatus.RASCUNHO);
        return copia;
    }

    public RevisaoValidacaoResponse validar(UUID id) {
        Release release = buscar(id);
        List<ReleaseItem> itens = itemRepository.findByReleaseIdOrderByOrdemAsc(id);

        List<String> pendencias = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        if (itens.isEmpty()) {
            pendencias.add("A release não possui itens cadastrados.");
        }
        if (release.getTitulo() == null || release.getTitulo().isBlank()) {
            pendencias.add("A release não possui título.");
        }

        if (release.getDataPrevista() == null) {
            alertas.add("A release não possui data prevista definida.");
        } else if (release.getDataPrevista().isBefore(LocalDate.now())) {
            alertas.add("A data prevista está no passado.");
        }
        if (release.getResumo() == null || release.getResumo().isBlank()) {
            alertas.add("A release não possui resumo.");
        }

        long totalCliente = itens.stream()
                .filter(i -> i.getVisibilidade() == VisibilidadeItem.TODOS
                        || i.getVisibilidade() == VisibilidadeItem.SUPORTE)
                .count();
        long totalInterno = itens.stream()
                .filter(i -> i.getVisibilidade() == VisibilidadeItem.TECNICO)
                .count();

        return new RevisaoValidacaoResponse(pendencias.isEmpty(), pendencias, alertas,
                totalCliente, totalInterno);
    }

    @Transactional
    public Page<Release> listar(String q, UUID produtoId, ReleaseStatus status,
                                String tipo, UUID responsavelId,
                                String sort, Pageable pageable) {
        Specification<Release> spec = (root, q2, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String filtro = "%" + q.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("titulo")), filtro),
                    cb.like(cb.lower(root.get("versao")), filtro),
                    cb.like(cb.lower(root.join("produto").get("nome")), filtro),
                    cb.like(cb.lower(root.join("produto").get("sigla")), filtro)
            ));
        }
        if (produtoId != null) {
            spec = spec.and((root, q2, cb) -> cb.equal(root.join("produto").get("id"), produtoId));
        }
        if (status != null) {
            spec = spec.and((root, q2, cb) -> cb.equal(root.get("status"), status));
        }
        if (tipo != null && !tipo.isBlank()) {
            spec = spec.and((root, q2, cb) -> cb.equal(root.get("tipo").as(String.class), tipo));
        }
        if (responsavelId != null) {
            spec = spec.and((root, q2, cb) -> cb.equal(root.get("responsavelId"), responsavelId));
        }

        return releaseRepository.findAll(spec, pageable);
    }

    @Transactional
    public Release buscar(UUID id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    }

    public List<ReleaseHistorico> buscarHistorico(UUID id) {
        buscar(id); // valida existência
        return historicoRepository.findByReleaseIdOrderByCreatedAtDesc(id);
    }

    public long contarItens(UUID releaseId) {
        return itemRepository.countByReleaseId(releaseId);
    }

    @Transactional
    public void excluir(UUID id) {
        Release release = buscar(id);
        if (release.getStatus() == ReleaseStatus.PUBLICADA) {
            throw new BusinessException("Não é possível excluir release publicada.");
        }
        releaseRepository.delete(release);
    }

    private void registrarHistorico(Release release, AcaoHistorico acao, String descricao,
                                    ReleaseStatus anterior, ReleaseStatus novo) {
        historicoRepository.save(new ReleaseHistorico(
                release.getId(), acao, descricao, anterior, novo, username()));
    }

    private AcaoHistorico resolverAcao(ReleaseStatus status) {
        return switch (status) {
            case EM_REVISAO   -> AcaoHistorico.ENVIADA_REVISAO;
            case APROVADA     -> AcaoHistorico.APROVADA;
            case PUBLICADA    -> AcaoHistorico.PUBLICADA;
            case CANCELADA    -> AcaoHistorico.CANCELADA;
            case RASCUNHO     -> AcaoHistorico.REABERTA;
            default           -> AcaoHistorico.EDITADA;
        };
    }

    private String username() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}

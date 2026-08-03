package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.request.AlterarStatusReleaseRequest;
import com.nexus.portal.releaseorchestrator.dto.request.ReleaseRequest;
import com.nexus.portal.releaseorchestrator.dto.response.RevisaoValidacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.releaseorchestrator.repository.ReleaseHistoricoRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseItemRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReleaseServiceTest {

    @Mock ReleaseRepository releaseRepository;
    @Mock ReleaseItemRepository itemRepository;
    @Mock ReleaseHistoricoRepository historicoRepository;
    @Mock ProdutoRhService produtoService;
    @InjectMocks ReleaseService service;

    UUID produtoId = UUID.randomUUID();
    ProdutoRh produto;

    @BeforeEach
    void setUp() {
        produto = new ProdutoRh("Sistema X", "SX", null, "#2563eb", null, true);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Deve criar release com sucesso")
    void deveCriarRelease() {
        ReleaseRequest request = new ReleaseRequest(produtoId, "5.13.0", "Título da release",
                TipoRelease.MINOR, null, null, null, null, null);
        when(produtoService.buscar(produtoId)).thenReturn(produto);
        when(releaseRepository.existsByProdutoIdAndVersao(any(), any())).thenReturn(false);
        when(releaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Release release = service.criar(request);

        assertThat(release.getVersao()).isEqualTo("5.13.0");
        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.RASCUNHO);
        verify(historicoRepository).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro ao criar release com versão duplicada")
    void deveLancarErroVersaoDuplicada() {
        ReleaseRequest request = new ReleaseRequest(produtoId, "5.13.0", "Título",
                TipoRelease.MINOR, null, null, null, null, null);
        when(produtoService.buscar(produtoId)).thenReturn(produto);
        when(releaseRepository.existsByProdutoIdAndVersao(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("versão");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar release inexistente")
    void deveLancarNotFoundAoBuscar() {
        UUID id = UUID.randomUUID();
        when(releaseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Deve alterar status seguindo o grafo de transições permitidas")
    void deveAlterarStatusPermitido() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.RASCUNHO);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));

        service.alterarStatus(id, new AlterarStatusReleaseRequest(ReleaseStatus.EM_DESENVOLVIMENTO, null));

        assertThat(release.getStatus()).isEqualTo(ReleaseStatus.EM_DESENVOLVIMENTO);
    }

    @Test
    @DisplayName("Deve lançar BusinessException em transição de status inválida")
    void deveLancarErroTransicaoInvalida() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.PUBLICADA);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.alterarStatus(id,
                new AlterarStatusReleaseRequest(ReleaseStatus.RASCUNHO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    @DisplayName("Deve bloquear publicação de release sem status APROVADA")
    void deveBloquearPublicacaoSemStatusAprovada() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.EM_REVISAO);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.publicar(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APROVADA");
    }

    @Test
    @DisplayName("Deve retornar validação inválida quando release não tem itens")
    void deveRetornarValidacaoInvalidaSemItens() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.APROVADA);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));
        when(itemRepository.findByReleaseIdOrderByOrdemAsc(id)).thenReturn(List.of());

        RevisaoValidacaoResponse resultado = service.validar(id);

        assertThat(resultado.valida()).isFalse();
        assertThat(resultado.pendencias()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve excluir release que não está publicada")
    void deveExcluirReleaseNaoPublicada() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.RASCUNHO);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));

        service.excluir(id);

        verify(releaseRepository).delete(release);
    }

    @Test
    @DisplayName("Deve impedir exclusão de release publicada")
    void deveImpedirExclusaoDeReleasePublicada() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.PUBLICADA);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("publicada");
        verify(releaseRepository, never()).delete(release);
    }

    @Test
    @DisplayName("Deve impedir edição de release publicada")
    void deveImpedirEdicaoDeReleasePublicada() {
        UUID id = UUID.randomUUID();
        Release release = criarRelease(ReleaseStatus.PUBLICADA);
        when(releaseRepository.findById(id)).thenReturn(Optional.of(release));
        when(produtoService.buscar(any())).thenReturn(produto);
        ReleaseRequest request = new ReleaseRequest(produtoId, "5.14.0", "Novo título",
                TipoRelease.MINOR, null, null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("editada");
    }

    private Release criarRelease(ReleaseStatus status) {
        return new Release(produto, "5.13.0", "Título", TipoRelease.MINOR,
                status, null, null, null, null);
    }
}

package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.dto.request.AlterarStatusProdutoRequest;
import br.com.softon.portal.releaseorchestrator.dto.request.ProdutoRhRequest;
import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import br.com.softon.portal.releaseorchestrator.repository.ProdutoRhRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdutoRhServiceTest {

    @Mock ProdutoRhRepository repository;
    @Mock ReleaseRepository releaseRepository;
    @InjectMocks ProdutoRhService service;

    @BeforeEach
    void setupSecurity() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Deve criar produto com sucesso")
    void deveCriarProduto() {
        ProdutoRhRequest request = new ProdutoRhRequest("Sistema X", "SX", null, "#2563eb", null, true, null, null, null, null);
        when(repository.existsBySiglaIgnoreCase("SX")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProdutoRh resultado = service.criar(request);

        assertThat(resultado.getNome()).isEqualTo("Sistema X");
        assertThat(resultado.getSigla()).isEqualTo("SX");
        verify(repository).save(any(ProdutoRh.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao criar com sigla duplicada")
    void deveLancarErroSiglaDuplicada() {
        ProdutoRhRequest request = new ProdutoRhRequest("Sistema Y", "SX", null, "#2563eb", null, true, null, null, null, null);
        when(repository.existsBySiglaIgnoreCase("SX")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sigla");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar produto inexistente")
    void deveLancarNotFoundAoBuscar() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Produto");
    }

    @Test
    @DisplayName("Deve alterar status do produto")
    void deveAlterarStatus() {
        UUID id = UUID.randomUUID();
        ProdutoRh produto = new ProdutoRh("Sistema X", "SX", null, "#2563eb", null, true);
        when(repository.findById(id)).thenReturn(Optional.of(produto));

        ProdutoRh resultado = service.alterarStatus(id, new AlterarStatusProdutoRequest(false));

        assertThat(resultado.isAtivo()).isFalse();
    }
}

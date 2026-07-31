package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.dto.request.ClienteRequest;
import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.repository.ClienteModuloRepository;
import br.com.softon.portal.docflow.repository.ClientePaginaRepository;
import br.com.softon.portal.docflow.repository.ClienteProjetoRepository;
import br.com.softon.portal.docflow.repository.ClienteRepository;
import br.com.softon.portal.docflow.repository.PreviewTokenRepository;
import br.com.softon.portal.docflow.repository.PublicacaoRepository;
import br.com.softon.portal.docflow.service.ModuloService;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.service.PaginaService;
import br.com.softon.portal.docflow.service.ProjetoService;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteServiceTest {

  @Mock ClienteRepository clienteRepository;
  @Mock ModuloService moduloService;
  @Mock PaginaService paginaService;
  @Mock ClienteModuloRepository clienteModuloRepository;
  @Mock ClientePaginaRepository clientePaginaRepository;
  @Mock ClienteProjetoRepository clienteProjetoRepository;
  @Mock ProjetoService projetoService;
  @Mock br.com.softon.rbac.service.EscopoResolver escopoResolver;
  @Mock PublicacaoRepository publicacaoRepository;
  @Mock PreviewTokenRepository previewTokenRepository;
  @Mock br.com.softon.rbac.service.AuditoriaService auditoriaService;
  @Mock SecurityContext securityContext;
  @Mock Authentication authentication;

  ClienteService service;

  @BeforeEach
  void setUp() {
    service = new ClienteService(clienteRepository, moduloService, paginaService,
        clienteModuloRepository, clientePaginaRepository, clienteProjetoRepository, projetoService,
        escopoResolver, publicacaoRepository, previewTokenRepository, auditoriaService,
        new ArquivoRemocaoService());

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("admin");
    SecurityContextHolder.setContext(securityContext);
    when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(java.util.Optional.empty());
    when(escopoResolver.podeAcessarCliente(any())).thenReturn(true);
    when(escopoResolver.podeEscreverEmCliente(any())).thenReturn(true);

    when(clienteRepository.save(any())).thenAnswer((Answer<Cliente>) inv -> inv.getArgument(0));
  }

  @Test
  void criar_comDadosValidos_deveCriarCliente() {
    when(clienteRepository.existsBySlug("acme-corp")).thenReturn(false);

    ClienteRequest request = new ClienteRequest("Acme Corp", "acme-corp", true, null, null);
    Cliente cliente = service.criar(request);

    assertThat(cliente.getNome()).isEqualTo("Acme Corp");
    assertThat(cliente.getSlug()).isEqualTo("acme-corp");
    assertThat(cliente.isAtivo()).isTrue();
    verify(clienteRepository).save(any(Cliente.class));
  }

  @Test
  void criar_comSlugDuplicado_deveLancarBusinessException() {
    when(clienteRepository.existsBySlug("acme-corp")).thenReturn(true);

    ClienteRequest request = new ClienteRequest("Acme Corp", "acme-corp", true, null, null);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
  }

  @Test
  void criar_comCorTemaInvalida_deveLancarBusinessException() {
    when(clienteRepository.existsBySlug(any())).thenReturn(false);

    ClienteRequest request = new ClienteRequest("Acme Corp", null, true, "cor-invalida", null);

    assertThatThrownBy(() -> service.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Cor de tema inválida");
  }

  @Test
  void criar_comCorTemaHexValida_deveCriarComTema() {
    when(clienteRepository.existsBySlug(any())).thenReturn(false);

    ClienteRequest request = new ClienteRequest("Acme Corp", null, true, "#4f46e5", "#f7f8fa");
    Cliente cliente = service.criar(request);

    assertThat(cliente.getTemaCorPrimaria()).isEqualTo("#4f46e5");
    assertThat(cliente.getTemaCorFundo()).isEqualTo("#f7f8fa");
  }

  @Test
  void buscar_comIdInexistente_deveLancarNotFoundException() {
    UUID id = UUID.randomUUID();
    when(clienteRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cliente não encontrado");
  }

  @Test
  void excluir_semPublicacoes_removeClienteTokensEAudita() {
    UUID id = UUID.randomUUID();
    Cliente cliente = new Cliente("Acme", "acme", true);
    java.security.Principal principal = () -> "admin";
    when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));
    when(publicacaoRepository.existsByCliente_Id(id)).thenReturn(false);

    service.excluir(id, principal);

    verify(previewTokenRepository).deleteByClienteId(id);
    verify(clienteRepository).delete(cliente);
    verify(auditoriaService).registrar("CLIENTE", id, "EXCLUIR", "Cliente excluído: Acme", principal);
  }

  @Test
  void excluir_comPublicacoesOrientaRemocaoPrevia() {
    UUID id = UUID.randomUUID();
    when(clienteRepository.findById(id)).thenReturn(Optional.of(new Cliente("Acme", "acme", true)));
    when(publicacaoRepository.existsByCliente_Id(id)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(id, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("publicações");

    verify(clienteRepository, never()).delete(any(Cliente.class));
  }

  @Test
  void vincularModulos_comIdsValidos_deveSalvarVinculos() {
    UUID clienteId = UUID.randomUUID();
    UUID moduloId1 = UUID.randomUUID();
    UUID moduloId2 = UUID.randomUUID();
    Cliente cliente = new Cliente("Acme", "acme", true);
    Projeto projeto = new Projeto("Proj", "proj", null, true);
    List<Modulo> modulos = List.of(
        new Modulo("Mod 1", "mod-1", null, 1, true, projeto),
        new Modulo("Mod 2", "mod-2", null, 2, true, projeto));

    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
    when(moduloService.buscarTodos(List.of(moduloId1, moduloId2))).thenReturn(modulos);

    service.vincularModulos(clienteId, List.of(moduloId1, moduloId2));

    verify(clienteModuloRepository).deleteByCliente_Id(clienteId);
    verify(clienteModuloRepository).saveAll(any());
  }

  @Test
  void vincularModulos_comListaVazia_deveRemoverVinculos() {
    UUID clienteId = UUID.randomUUID();
    Cliente cliente = new Cliente("Acme", "acme", true);
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

    service.vincularModulos(clienteId, List.of());

    verify(clienteModuloRepository).deleteByCliente_Id(clienteId);
    verify(clienteModuloRepository, org.mockito.Mockito.never()).saveAll(any());
  }

  @Test
  void copiarVinculos_comMesmoCliente_deveLancarBusinessException() {
    UUID clienteId = UUID.randomUUID();

    assertThatThrownBy(() -> service.copiarVinculos(clienteId, clienteId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("diferentes");
  }

  @Test
  void atualizar_comSlugDuplicadoEmOutroCliente_deveLancarBusinessException() {
    UUID id = UUID.randomUUID();
    Cliente cliente = new Cliente("Acme Corp", "acme-corp", true);
    when(clienteRepository.findById(id)).thenReturn(Optional.of(cliente));
    when(clienteRepository.existsBySlugAndIdNot("novo-slug", id)).thenReturn(true);

    ClienteRequest request = new ClienteRequest("Acme Corp", "novo-slug", true, null, null);

    assertThatThrownBy(() -> service.atualizar(id, request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
  }
}

package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.entity.Grupo;
import br.com.softon.rbac.entity.Permissao;
import br.com.softon.rbac.repository.GrupoRepository;
import br.com.softon.rbac.repository.PermissaoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

  @Mock GrupoRepository grupoRepository;
  @Mock PermissaoRepository permissaoRepository;

  RbacService rbacService;

  @BeforeEach
  void setUp() {
    rbacService = new RbacService(grupoRepository, permissaoRepository);
  }

  @Test
  void permissoesDoUsuario_deveUnirPermissoesDosGruposAtivos() {
    UUID usuarioId = UUID.randomUUID();
    UUID permId = UUID.randomUUID();

    Grupo grupo = new Grupo("EDITOR", "Editores", null, true);
    grupo.atualizarPermissaoIds(List.of(permId));

    Permissao permissao = mock(Permissao.class);
    when(permissao.getCodigo()).thenReturn("CLIENTE:LER");
    when(permissao.isAtivo()).thenReturn(true);

    when(grupoRepository.findAtivosComUsuario(usuarioId)).thenReturn(List.of(grupo));
    when(permissaoRepository.findByIdIn(anyCollection())).thenReturn(List.of(permissao));

    assertThat(rbacService.permissoesDoUsuario(usuarioId)).containsExactly("CLIENTE:LER");
  }
}

package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.service.EscopoResolver;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicacaoEventServiceTest {

  @Mock EscopoResolver escopoResolver;

  @Test
  void inscrever_fotografaOEscopoDeClientesDoAssinante() {
    when(escopoResolver.clientesPermitidosDoUsuarioAtual()).thenReturn(Optional.empty());

    new PublicacaoEventService(escopoResolver).inscrever();

    verify(escopoResolver).clientesPermitidosDoUsuarioAtual();
  }

  @Test
  void podeReceber_assinanteRestrito_soRecebeOsClientesDaWhitelist() {
    UUID permitido = UUID.randomUUID();
    UUID deOutroCliente = UUID.randomUUID();
    Optional<Set<UUID>> escopo = Optional.of(Set.of(permitido));

    assertThat(SseBroadcaster.podeReceber(escopo, permitido)).isTrue();
    assertThat(SseBroadcaster.podeReceber(escopo, deOutroCliente)).isFalse();
  }

  @Test
  void podeReceber_assinanteSemRestricao_recebeQualquerCliente() {
    assertThat(SseBroadcaster.podeReceber(Optional.empty(), UUID.randomUUID())).isTrue();
  }

  @Test
  void podeReceber_whitelistVazia_naoRecebeNada() {
    assertThat(SseBroadcaster.podeReceber(Optional.of(Set.of()), UUID.randomUUID())).isFalse();
  }

  @Test
  void podeReceber_eventoSemCliente_vaiParaTodos() {
    assertThat(SseBroadcaster.podeReceber(Optional.of(Set.of(UUID.randomUUID())), null)).isTrue();
  }
}

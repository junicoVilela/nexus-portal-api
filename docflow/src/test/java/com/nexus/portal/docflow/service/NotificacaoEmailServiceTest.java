package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Modulo;
import com.nexus.portal.docflow.entity.Pagina;
import com.nexus.portal.docflow.entity.Projeto;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPagina;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoEmailServiceTest {

  @Mock JavaMailSender mailSender;
  @Mock com.nexus.identityaccess.service.UsuarioService usuarioService;

  @Test
  void naoEnviaQuandoDesabilitado() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "admin@x.com", false);

    service.notificarPaginaEmRevisao(paginaComTitulo("X"));
    service.notificarPublicacaoGerada(publicacaoBasica());

    verifyNoInteractions(mailSender);
  }

  @Test
  void naoEnviaAdminQuandoAdminEmailVazio() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "", true);

    service.notificarPaginaEmRevisao(paginaComTitulo("X"));

    verifyNoInteractions(mailSender);
  }

  @Test
  void revisao_enviaEmailParaAdminComTituloECodigo() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "admin@x.com", true);

    service.notificarPaginaEmRevisao(paginaComTitulo("Login"));

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage msg = captor.getValue();
    assertThat(msg.getTo()).containsExactly("admin@x.com");
    assertThat(msg.getSubject()).contains("Login");
    assertThat(msg.getText()).contains("LOGIN");
  }

  @Test
  void aprovada_enviaEmailParaEditor() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "admin@x.com", true);
    when(usuarioService.emailDoUsername("editor")).thenReturn(java.util.Optional.of("editor@x.com"));

    service.notificarPaginaAprovada(paginaComTitulo("Home"), "editor");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    assertThat(captor.getValue().getTo()).containsExactly("editor@x.com");
    assertThat(captor.getValue().getSubject()).contains("Home");
  }

  @Test
  void aprovada_pulaSeEditorNaoTemEmailCadastrado() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "admin@x.com", true);
    when(usuarioService.emailDoUsername(any())).thenReturn(java.util.Optional.empty());

    service.notificarPaginaAprovada(paginaComTitulo("Home"), "editor-sem-email");

    verifyNoInteractions(mailSender);
  }

  @Test
  void falhaAoEnviarNaoQuebra() {
    NotificacaoEmailService service = new NotificacaoEmailService(mailSender, usuarioService, "admin@x.com", true);
    doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

    // Não deve propagar
    service.notificarPaginaEmRevisao(paginaComTitulo("X"));
  }

  private Pagina paginaComTitulo(String titulo) {
    Projeto proj = new Projeto("P", "p", null, true);
    Modulo mod = new Modulo("M", "m", null, 1, true, proj);
    Pagina p = new Pagina(titulo, titulo.toLowerCase(), titulo.toUpperCase(), null, null, 1, true, mod, null);
    try {
      Field f = Pagina.class.getDeclaredField("status");
      f.setAccessible(true);
      f.set(p, StatusPagina.EM_REVISAO);
      Field u = Pagina.class.getSuperclass().getDeclaredField("updatedBy");
      u.setAccessible(true);
      u.set(p, "editor");
    } catch (Exception ignored) {
    }
    return p;
  }

  private Publicacao publicacaoBasica() {
    Cliente c = new Cliente("ACME", "acme", true);
    return new Publicacao(c, "1.0.0", null);
  }
}

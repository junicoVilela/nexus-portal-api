package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.Publicacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoEmailService {

  private static final Logger log = LoggerFactory.getLogger(NotificacaoEmailService.class);

  private final JavaMailSender mailSender;
  private final String adminEmail;
  private final boolean emailHabilitado;

  public NotificacaoEmailService(
      JavaMailSender mailSender,
      @Value("${docflow.notificacoes.admin-email:}") String adminEmail,
      @Value("${docflow.notificacoes.habilitado:false}") boolean emailHabilitado) {
    this.mailSender = mailSender;
    this.adminEmail = adminEmail;
    this.emailHabilitado = emailHabilitado;
  }

  @Async
  public void notificarPaginaEmRevisao(Pagina pagina) {
    if (!emailHabilitado || adminEmail.isBlank()) return;
    enviar(adminEmail,
        "[Manual] Página enviada para revisão: " + pagina.getTitulo(),
        "A página '" + pagina.getTitulo() + "' (cód: " + pagina.getCodigoTela()
            + ") foi enviada para revisão por " + pagina.getUpdatedBy() + ".");
  }

  @Async
  public void notificarPaginaAprovada(Pagina pagina, String emailEditor) {
    if (!emailHabilitado) return;
    if (emailEditor != null && !emailEditor.isBlank()) {
      enviar(emailEditor,
          "[Manual] Sua página foi aprovada: " + pagina.getTitulo(),
          "A página '" + pagina.getTitulo() + "' foi aprovada e está pronta para publicação.");
    }
  }

  @Async
  public void notificarPublicacaoGerada(Publicacao publicacao) {
    if (!emailHabilitado || adminEmail.isBlank()) return;
    String status = publicacao.getStatus().name();
    enviar(adminEmail,
        "[Manual] Publicação " + status + " - " + publicacao.getCliente().getNome()
            + " v" + publicacao.getVersao(),
        "A geração da publicação para o cliente '" + publicacao.getCliente().getNome()
            + "' versão " + publicacao.getVersao() + " finalizou com status: " + status + ".\n"
            + "Páginas: " + publicacao.getQuantidadePaginas()
            + " | Módulos: " + publicacao.getQuantidadeModulos());
  }

  private void enviar(String destinatario, String assunto, String corpo) {
    try {
      SimpleMailMessage msg = new SimpleMailMessage();
      msg.setTo(destinatario);
      msg.setSubject(assunto);
      msg.setText(corpo);
      mailSender.send(msg);
    } catch (Exception e) {
      log.warn("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
    }
  }
}

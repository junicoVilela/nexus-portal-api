package com.nexus.portal.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

  @Test
  void uploadTooLarge_retorna413ComMensagemAcionavel() {
    var handler = new GlobalExceptionHandler();

    var response = handler.uploadTooLarge(new MaxUploadSizeExceededException(15L * 1024 * 1024, null));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("O arquivo excede o limite permitido de 15 MB.");
  }

  @Test
  void portaDuplicada_retorna409ComMensagemAcionavel() {
    var handler = new GlobalExceptionHandler();
    var cause = new RuntimeException(
        "ERROR: duplicate key value violates unique constraint \"uq_tb_reserva_porta_ativa\"");
    var response = handler.dataIntegrity(new DataIntegrityViolationException("insert", cause));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).contains("porta já está reservada");
  }
}

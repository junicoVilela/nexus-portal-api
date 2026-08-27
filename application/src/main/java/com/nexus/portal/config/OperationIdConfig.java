package com.nexus.portal.config;

import java.util.Locale;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dá nome estável às operações do OpenAPI.
 *
 * <p>Por padrão o springdoc usa o nome do método e, em colisão, acrescenta um
 * sufixo numérico pela ordem de varredura: {@code listar}, {@code listar_1},
 * {@code listar18}. Como a ordem muda quando qualquer controller ganha um
 * método, todo endpoint novo renomeia operações alheias — e o SDK gerado no
 * frontend precisa ser reescrito inteiro.
 *
 * <p>Aqui o id passa a ser {@code <controller sem o sufixo Controller><Método>},
 * derivado só da classe e do método: previsível, único e imune a reordenação.
 */
@Configuration
public class OperationIdConfig {

  @Bean
  OperationCustomizer operationIdEstavel() {
    return (operation, handlerMethod) -> {
      Class<?> tipo = handlerMethod.getBeanType();
      String controller = tipo.getSimpleName().replaceAll("Controller$", "");
      operation.setOperationId(modulo(tipo) + capitalizar(controller)
          + capitalizar(handlerMethod.getMethod().getName()));
      return operation;
    };
  }

  /**
   * O nome simples do controller não basta: {@code ClienteController} existe no
   * DocFlow e no Release Orchestrator. O módulo entra no id para o nome não
   * depender da ordem em que as classes foram varridas.
   */
  private String modulo(Class<?> tipo) {
    String pacote = tipo.getPackageName();
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("com\\.nexus\\.(?:portal\\.)?([a-z]+)").matcher(pacote);
    return m.find() ? m.group(1) : "api";
  }

  private String capitalizar(String valor) {
    return valor.isEmpty() ? valor : valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1);
  }
}

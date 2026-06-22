package br.com.softon.portal.config;

import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Propaga o MDC (incluindo {@code correlationId}) do thread chamador para o
 * worker que executa o {@code @Async}. Sem isso, logs da geração de entrega
 * perdem o correlationId da requisição que disparou a geração.
 *
 * F4 observability — usado com {@code logback-spring.xml} profile prod.
 */
@Configuration
public class AsyncMdcConfig implements AsyncConfigurer {

  @Bean(name = "applicationTaskExecutor")
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("portal-async-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SimpleAsyncUncaughtExceptionHandler();
  }

  static class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      return () -> {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (contextMap != null) MDC.setContextMap(contextMap);
        try {
          runnable.run();
        } finally {
          if (previous != null) MDC.setContextMap(previous);
          else MDC.clear();
        }
      };
    }
  }
}

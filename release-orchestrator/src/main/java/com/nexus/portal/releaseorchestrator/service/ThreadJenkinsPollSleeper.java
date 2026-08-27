package com.nexus.portal.releaseorchestrator.service;

import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ThreadJenkinsPollSleeper implements JenkinsPollSleeper {

  @Override
  public void sleep(Duration duration) throws InterruptedException {
    Thread.sleep(duration.toMillis());
  }
}

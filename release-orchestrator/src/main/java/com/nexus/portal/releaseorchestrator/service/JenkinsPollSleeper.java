package com.nexus.portal.releaseorchestrator.service;

import java.time.Duration;

/** Espera entre polls do Jenkins. Testes substituem por no-op. */
public interface JenkinsPollSleeper {

  void sleep(Duration duration) throws InterruptedException;
}

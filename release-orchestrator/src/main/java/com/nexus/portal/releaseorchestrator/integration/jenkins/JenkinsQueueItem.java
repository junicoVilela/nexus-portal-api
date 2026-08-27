package com.nexus.portal.releaseorchestrator.integration.jenkins;

/** Item da fila Jenkins após disparar o build. */
public record JenkinsQueueItem(boolean cancelled, Integer executableNumber, String executableUrl) {}

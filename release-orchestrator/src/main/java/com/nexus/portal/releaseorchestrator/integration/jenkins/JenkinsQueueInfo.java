package com.nexus.portal.releaseorchestrator.integration.jenkins;

/** Item enfileirado após POST /build ou /buildWithParameters. */
public record JenkinsQueueInfo(String queueUrl) {}

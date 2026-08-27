package com.nexus.portal.releaseorchestrator.integration.github;

/** Tag Git retornada por GET /repos/{owner}/{repo}/tags. */
public record GitHubTag(String name, String sha) {}

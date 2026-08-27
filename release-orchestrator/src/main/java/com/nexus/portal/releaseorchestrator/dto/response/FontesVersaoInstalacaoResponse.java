package com.nexus.portal.releaseorchestrator.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Opções de versão na ficha da instalação: branch padrão do produto,
 * última versão (maior número) e tags específicas. Inclui os alvos de
 * build (módulos / outros produtos) para gerar vários WAR/JAR em
 * {@code artifacts/}.
 */
public record FontesVersaoInstalacaoResponse(
    UUID instalacaoId,
    UUID produtoId,
    String versaoInstalada,
    boolean jenkinsConfigurado,
    boolean githubConfigurado,
    String jenkinsJob,
    String aviso,
    String githubErro,
    OpcaoVersao versaoAtual,
    OpcaoVersao ultimaGerada,
    List<OpcaoVersao> tags,
    List<AlvoBuild> alvos,
    List<BuildArtefatoStatus> buildsRecentes) {

  public FontesVersaoInstalacaoResponse {
    if (tags == null) tags = List.of();
    if (alvos == null) alvos = List.of();
    if (buildsRecentes == null) buildsRecentes = List.of();
  }

  public record OpcaoVersao(
      String origem,
      String tag,
      String versao,
      UUID releaseId,
      boolean selecionavel,
      String titulo,
      String aviso,
      Integer totalAssets) {}

  public record AlvoBuild(
      String id,
      String codigo,
      String nome,
      String tipo,
      UUID produtoId,
      String produtoSigla,
      String jenkinsJob,
      String nomeArquivo,
      String padraoAsset,
      boolean doProdutoDaInstalacao,
      boolean selecionadoPadrao) {}

  public record BuildArtefatoStatus(
      UUID id,
      String alvoId,
      String produtoSigla,
      String jenkinsJob,
      String status,
      String nomeArquivo,
      String mensagem,
      OffsetDateTime createdAt) {}
}

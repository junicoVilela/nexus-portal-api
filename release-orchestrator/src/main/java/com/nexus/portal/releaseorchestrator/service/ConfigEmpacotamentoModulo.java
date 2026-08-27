package com.nexus.portal.releaseorchestrator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;

/**
 * Subconjunto de {@code ModuloProduto.configEspecifica} usado no disparo de
 * build e na cópia para {@code artifacts/} da instalação.
 *
 * <pre>{
 *   "jenkinsJob": "ld-v5-build",
 *   "padraoAsset": "ldv4.war",
 *   "nomeArtefato": "ldv4.war",
 *   "destinoPacote": "artifacts/",
 *   "selecionadoPadrao": true
 * }</pre>
 *
 * {@code jenkinsJob} omitido herda o job do produto; string vazia = sem job
 * (artefato ainda não ligado a um pipeline).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigEmpacotamentoModulo(
    String jenkinsJob,
    String padraoAsset,
    String nomeArtefato,
    String destinoPacote,
    Boolean selecionadoPadrao) {

  public static ConfigEmpacotamentoModulo vazio() {
    return new ConfigEmpacotamentoModulo(null, null, null, null, null);
  }

  public static ConfigEmpacotamentoModulo de(String json, ObjectMapper mapper) {
    if (json == null || json.isBlank() || mapper == null) {
      return vazio();
    }
    try {
      ConfigEmpacotamentoModulo cfg = mapper.readValue(json, ConfigEmpacotamentoModulo.class);
      return cfg == null ? vazio() : cfg;
    } catch (JsonProcessingException e) {
      return vazio();
    }
  }

  public String jobOu(String jobDoProduto) {
    if (jenkinsJob != null) {
      return jenkinsJob.isBlank() ? null : jenkinsJob.trim();
    }
    return jobDoProduto;
  }

  public boolean marcadoPadrao(boolean temJob) {
    if (!temJob) {
      return false;
    }
    return selecionadoPadrao == null || selecionadoPadrao;
  }

  public String padraoAssetOuDefault(TipoModulo tipo) {
    if (padraoAsset != null && !padraoAsset.isBlank()) {
      return padraoAsset.trim();
    }
    String nome = nomeDestino();
    if (nome != null) {
      return nome;
    }
    if (tipo == TipoModulo.BATCH) {
      return "*.jar,*.zip";
    }
    if (tipo == TipoModulo.BANCO) {
      return "*.sql,*.zip";
    }
    if (tipo == TipoModulo.KETTLE) {
      return "*.ktr,*.kjb,*.zip";
    }
    return "*.war,*.jar,*.ear,*.zip";
  }

  /** Nome fixo em {@code artifacts/}, ou null para manter o nome do Jenkins. */
  public String nomeDestino() {
    if (nomeArtefato != null && !nomeArtefato.isBlank()) {
      return ArtefatoBuildMatcher.nomeArquivo(nomeArtefato.trim());
    }
    if (destinoPacote == null || destinoPacote.isBlank()) {
      return null;
    }
    String n = ArtefatoBuildMatcher.nomeArquivo(destinoPacote.trim());
    return n.contains(".") ? n : null;
  }
}

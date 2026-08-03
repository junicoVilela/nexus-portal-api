package com.nexus.portal.releaseorchestrator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuração específica de módulo KETTLE armazenada como JSON no campo
 * {@code ModuloProduto.configEspecifica}.
 *
 * Exemplo:
 * <pre>{
 *   "caminhoRepo": "kettle/nexus-ld",
 *   "incluirDependencias": true
 * }</pre>
 *
 * Defaults quando ausentes:
 * <ul>
 *   <li>caminhoRepo: "" (raiz do repo)</li>
 *   <li>incluirDependencias: false — opt-in</li>
 * </ul>
 *
 * <p>Quando {@code incluirDependencias=true}, o
 * {@link GithubDeltaKettleService} parseia cada .ktr/.kjb alterado e
 * inclui transformações/subjobs referenciados (1 nível de profundidade).
 * Referências via variáveis Kettle ({@code ${PDI_HOME}/x.ktr}) são
 * ignoradas — só paths literais resolvem.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigKettleModulo(
    String caminhoRepo,
    boolean incluirDependencias) {

  public ConfigKettleModulo {
    if (caminhoRepo == null) caminhoRepo = "";
  }

  public static ConfigKettleModulo padrao() {
    return new ConfigKettleModulo("", false);
  }

  public static ConfigKettleModulo de(String json, ObjectMapper mapper) {
    if (json == null || json.isBlank()) return padrao();
    try {
      return mapper.readValue(json, ConfigKettleModulo.class);
    } catch (JsonProcessingException e) {
      return padrao();
    }
  }

  public boolean dentroDoEscopo(String filename) {
    if (caminhoRepo.isEmpty()) return true;
    String prefixo = caminhoRepo.endsWith("/") ? caminhoRepo : caminhoRepo + "/";
    return filename.startsWith(prefixo);
  }
}

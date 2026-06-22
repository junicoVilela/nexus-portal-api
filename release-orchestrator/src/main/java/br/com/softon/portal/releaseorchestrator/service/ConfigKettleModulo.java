package br.com.softon.portal.releaseorchestrator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuração específica de módulo KETTLE armazenada como JSON no campo
 * {@code ModuloProduto.configEspecifica}.
 *
 * Exemplo:
 * <pre>{ "caminhoRepo": "kettle/dtec-ld" }</pre>
 *
 * Default quando ausente: caminhoRepo = "" (raiz).
 *
 * <p>Dependências entre transformações/jobs (incluirDependencias) ficam para
 * uma iteração futura — exigiria parser de .ktr/.kjb para resolver subjobs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigKettleModulo(String caminhoRepo) {

  public ConfigKettleModulo {
    if (caminhoRepo == null) caminhoRepo = "";
  }

  public static ConfigKettleModulo padrao() {
    return new ConfigKettleModulo("");
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

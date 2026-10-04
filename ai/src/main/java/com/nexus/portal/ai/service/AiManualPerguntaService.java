package com.nexus.portal.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.ai.config.AiProperties;
import com.nexus.portal.ai.dto.response.AiManualRespostaResponse;
import com.nexus.portal.ai.dto.response.AiManualRespostaResponse.Citacao;
import com.nexus.portal.ai.entity.AiManualPergunta;
import com.nexus.portal.ai.entity.AiManualPergunta.Modo;
import com.nexus.portal.ai.entity.AiManualPergunta.Origem;
import com.nexus.portal.ai.integration.docflow.DocFlowAiBridge;
import com.nexus.portal.ai.prompt.AiPromptBuilder;
import com.nexus.portal.ai.provider.LlmProvider;
import com.nexus.portal.ai.repository.AiManualPerguntaRepository;
import com.nexus.portal.docflow.service.ManualBusca;
import com.nexus.portal.docflow.service.ManualBusca.Resultado;
import com.nexus.portal.docflow.service.ManualCorpusService.Corpus;
import com.nexus.portal.docflow.service.ManualCorpusService.Documento;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Perguntar ao manual publicado (Onda E, INT-501/502/505/506).
 *
 * <pre>
 * pergunta → busca no snapshot publicado (ManualBusca)
 *   sem trecho que cubra a pergunta   → NAO_SEI (a IA nem é chamada)
 *   IA ligada                          → resposta só com os trechos + citação validada (IA)
 *   IA desligada ou falhou             → os trechos encontrados, sem texto gerado (TRECHOS)
 * </pre>
 *
 * Cada pergunta fica em {@code tb_ai_manual_pergunta} sem identificar quem perguntou; o log
 * padrão só leva publicação, modo e telas citadas.
 */
@Service
public class AiManualPerguntaService {

  private static final Logger log = LoggerFactory.getLogger(AiManualPerguntaService.class);
  static final int TRECHOS_PARA_IA = 6;
  static final int TRECHOS_SEM_IA = 3;
  private static final int MAX_CHARS_TRECHO = 1_500;

  private final DocFlowAiBridge docFlowAiBridge;
  private final LlmProvider llmProvider;
  private final AiProperties properties;
  private final AiManualPerguntaRepository perguntaRepository;
  private final ObjectMapper objectMapper;

  public AiManualPerguntaService(
      DocFlowAiBridge docFlowAiBridge,
      LlmProvider llmProvider,
      AiProperties properties,
      AiManualPerguntaRepository perguntaRepository,
      ObjectMapper objectMapper) {
    this.docFlowAiBridge = docFlowAiBridge;
    this.llmProvider = llmProvider;
    this.properties = properties;
    this.perguntaRepository = perguntaRepository;
    this.objectMapper = objectMapper;
  }

  /** Pelo portal: qualquer publicação concluída (quem tem PUBLICACAO:LER). */
  public AiManualRespostaResponse perguntarPublicacao(UUID publicacaoId, String pergunta) {
    return responder(docFlowAiBridge.corpusDaPublicacao(publicacaoId), pergunta, Origem.PORTAL);
  }

  /** Pelo leitor (prévia, app do cliente): manual vigente do cliente da chave ou do link de prévia. */
  public AiManualRespostaResponse perguntarComToken(String token, String origem, String pergunta) {
    return responder(docFlowAiBridge.corpusDoToken(token, origem), pergunta, Origem.LEITOR);
  }

  AiManualRespostaResponse responder(Corpus corpus, String pergunta, Origem origem) {
    long inicio = System.nanoTime();
    String texto = pergunta.strip();
    ManualBusca.Resposta busca = ManualBusca.buscar(corpus, texto, TRECHOS_PARA_IA);
    AiManualRespostaResponse resposta;
    if (!busca.encontrou()) {
      resposta = naoSei(corpus);
    } else if (properties.enabled()) {
      resposta = comIa(corpus, texto, busca.resultados());
    } else {
      resposta = soTrechos(corpus, busca.resultados());
    }
    long latencia = (System.nanoTime() - inicio) / 1_000_000;
    List<String> citados = resposta.citacoes().stream().map(Citacao::codigoTela).distinct().toList();
    Double cobertura = busca.resultados().isEmpty() ? null : busca.resultados().getFirst().cobertura();
    perguntaRepository.save(new AiManualPergunta(corpus.publicacaoId(), origem, texto, resposta.modo(), citados,
        cobertura, latencia));
    log.info("ai.manual.pergunta publicacao={} origem={} modo={} citadas={} ms={}",
        corpus.publicacaoId(), origem, resposta.modo(), citados, latencia);
    return resposta;
  }

  private AiManualRespostaResponse comIa(Corpus corpus, String pergunta, List<Resultado> resultados) {
    Map<String, Resultado> porCodigo = new LinkedHashMap<>();
    resultados.forEach(r -> porCodigo.putIfAbsent(r.secao().documento().codigoTela(), r));
    var prompt = AiPromptBuilder.responderManual(corpus.rotulo(), pergunta, trechos(resultados));
    try {
      String conteudo = llmProvider.completarEstruturado(prompt.system(), prompt.user(), schema()).content();
      JsonNode json = objectMapper.readTree(conteudo);
      Set<String> citados = new LinkedHashSet<>();
      json.path("citacoes").forEach(c -> citados.add(c.asText()));
      citados.retainAll(porCodigo.keySet()); // só telas que estavam nos trechos
      String texto = json.path("resposta").asText("").strip();
      if (!json.path("encontrou").asBoolean(false) || texto.isEmpty() || citados.isEmpty()) {
        return naoSei(corpus);
      }
      List<Citacao> citacoes = new ArrayList<>();
      citados.forEach(codigo -> citacoes.add(citacao(porCodigo.get(codigo))));
      return new AiManualRespostaResponse(corpus.rotulo(), corpus.versao(), Modo.IA, texto, citacoes);
    } catch (JsonProcessingException | RuntimeException ex) {
      log.warn("ai.manual.pergunta falha da IA, devolvendo trechos: {}", ex.getMessage());
      return soTrechos(corpus, resultados);
    }
  }

  private AiManualRespostaResponse soTrechos(Corpus corpus, List<Resultado> resultados) {
    List<Citacao> citacoes = resultados.stream().limit(TRECHOS_SEM_IA).map(AiManualPerguntaService::citacao).toList();
    return new AiManualRespostaResponse(corpus.rotulo(), corpus.versao(), Modo.TRECHOS,
        "Encontrei estes trechos no manual:", citacoes);
  }

  private static AiManualRespostaResponse naoSei(Corpus corpus) {
    return new AiManualRespostaResponse(corpus.rotulo(), corpus.versao(), Modo.NAO_SEI,
        "Não encontrei isso no " + corpus.rotulo() + ". Tente outras palavras ou procure a tela pelo menu.",
        List.of());
  }

  private static Citacao citacao(Resultado resultado) {
    Documento doc = resultado.secao().documento();
    return new Citacao(doc.codigoTela(), doc.titulo(), resultado.secao().titulo(), doc.caminho(), doc.url(),
        resumir(resultado.secao().texto(), 400));
  }

  /** Cada trecho com o código da tela em linha própria (a IA cita por ele). */
  static String trechos(List<Resultado> resultados) {
    StringBuilder out = new StringBuilder();
    for (Resultado r : resultados) {
      Documento doc = r.secao().documento();
      out.append("codigoTela: ").append(doc.codigoTela()).append('\n')
          .append("tela: ").append(doc.titulo())
          .append(r.secao().titulo() == null ? "" : " — " + r.secao().titulo()).append('\n')
          .append("caminho: ").append(doc.caminho()).append('\n')
          .append(resumir(r.secao().texto(), MAX_CHARS_TRECHO)).append("\n\n");
    }
    return out.toString().strip();
  }

  private static String resumir(String texto, int max) {
    return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
  }

  private JsonNode schema() throws JsonProcessingException {
    return objectMapper.readTree("""
        {
          "type":"object",
          "additionalProperties":false,
          "required":["encontrou","resposta","citacoes"],
          "properties":{
            "encontrou":{"type":"boolean"},
            "resposta":{"type":"string"},
            "citacoes":{"type":"array","maxItems":6,"items":{"type":"string"}}
          }
        }
        """);
  }
}

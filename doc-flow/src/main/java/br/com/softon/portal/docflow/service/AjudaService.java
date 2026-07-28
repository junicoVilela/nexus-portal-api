package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.request.AjudaConteudoRequest;
import br.com.softon.portal.docflow.dto.request.AjudaEventoRequest;
import br.com.softon.portal.docflow.dto.response.AjudaMetricaItemResponse;
import br.com.softon.portal.docflow.dto.response.AjudaMetricasResponse;
import br.com.softon.portal.docflow.entity.AjudaConteudo;
import br.com.softon.portal.docflow.entity.AjudaEvento;
import br.com.softon.portal.docflow.entity.TipoAjudaEvento;
import br.com.softon.portal.docflow.entity.TipoAjudaConteudo;
import br.com.softon.portal.docflow.entity.TipoAjudaMedia;
import br.com.softon.portal.docflow.repository.AjudaConteudoRepository;
import br.com.softon.portal.docflow.repository.AjudaEventoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.net.URI;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AjudaService {

  private static final int JANELA_METRICAS_DIAS = 30;
  private static final int LIMITE_RANKING = 6;
  private static final int JANELA_DEDUPLICACAO_SEGUNDOS = 10;

  private final AjudaConteudoRepository conteudoRepository;
  private final AjudaEventoRepository eventoRepository;

  @Transactional(readOnly = true)
  public List<AjudaConteudo> listar(String busca, String rota, boolean incluirInativos) {
    String termoNormalizado = normalizar(busca);
    String rotaNormalizada = textoOpcional(rota);
    List<AjudaConteudo> conteudos = incluirInativos
        ? conteudoRepository.findAllByOrderByOrdemAscTituloAsc()
        : conteudoRepository.findByAtivoTrueOrderByOrdemAscTituloAsc();
    return conteudos.stream()
        .filter(item -> termoNormalizado == null || corresponde(item, termoNormalizado))
        .filter(item -> rotaNormalizada == null || item.getRotaContexto() == null
            || rotaNormalizada.startsWith(item.getRotaContexto()))
        .toList();
  }

  @Transactional
  public AjudaConteudo criar(AjudaConteudoRequest request) {
    String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
    if (conteudoRepository.existsByCodigo(codigo)) {
      throw new BusinessException("Já existe um conteúdo de ajuda com este código.");
    }
    AjudaConteudo conteudo = new AjudaConteudo(codigo, request.tipo(), request.titulo().trim());
    atualizar(conteudo, request);
    return conteudoRepository.save(conteudo);
  }

  @Transactional
  public AjudaConteudo atualizar(UUID id, AjudaConteudoRequest request) {
    AjudaConteudo conteudo = buscar(id);
    if (!conteudo.getCodigo().equalsIgnoreCase(request.codigo().trim())) {
      throw new BusinessException("O código do conteúdo não pode ser alterado.");
    }
    if (conteudo.getTipo() == TipoAjudaConteudo.JORNADA
        && request.tipo() != TipoAjudaConteudo.JORNADA
        && conteudoRepository.existsByJornadaCodigo(conteudo.getCodigo())) {
      throw new BusinessException("Remova ou mova as etapas antes de alterar o tipo da jornada.");
    }
    atualizar(conteudo, request);
    return conteudo;
  }

  @Transactional
  public void excluir(UUID id) {
    AjudaConteudo conteudo = buscar(id);
    if (conteudo.getTipo() == TipoAjudaConteudo.JORNADA
        && conteudoRepository.existsByJornadaCodigo(conteudo.getCodigo())) {
      throw new BusinessException("A jornada possui etapas vinculadas e não pode ser excluída.");
    }
    conteudoRepository.delete(conteudo);
  }

  @Transactional
  public void registrar(AjudaEventoRequest request, String usuario) {
    String conteudoCodigo = textoOpcional(request.conteudoCodigo());
    String termo = textoOpcional(request.termo());
    String rota = textoOpcional(request.rota());
    String sessaoId = textoOpcional(request.sessaoId());
    OffsetDateTime desde = OffsetDateTime.now().minusSeconds(JANELA_DEDUPLICACAO_SEGUNDOS);
    if (eventoRepository.contarEventoEquivalente(
        request.tipo(), sessaoId, conteudoCodigo, termo, rota, desde) > 0) {
      return;
    }
    eventoRepository.save(new AjudaEvento(request.tipo(), conteudoCodigo, termo, rota, sessaoId,
        request.resultadoQuantidade(), textoOpcional(usuario)));
  }

  @Transactional(readOnly = true)
  public AjudaMetricasResponse metricas() {
    OffsetDateTime desde = OffsetDateTime.now().minusDays(JANELA_METRICAS_DIAS);
    long total = eventoRepository.countByCreatedAtGreaterThanEqual(desde);
    long buscas = eventoRepository.countByTipoAndCreatedAtGreaterThanEqual(TipoAjudaEvento.BUSCA, desde)
        + eventoRepository.countByTipoAndCreatedAtGreaterThanEqual(TipoAjudaEvento.BUSCA_SEM_RESULTADO, desde);
    long semResultado = eventoRepository.countByTipoAndCreatedAtGreaterThanEqual(
        TipoAjudaEvento.BUSCA_SEM_RESULTADO, desde);
    long iniciados = eventoRepository.countByTipoAndCreatedAtGreaterThanEqual(TipoAjudaEvento.TOUR_INICIADO, desde);
    long concluidos = eventoRepository.countByTipoAndCreatedAtGreaterThanEqual(TipoAjudaEvento.TOUR_CONCLUIDO, desde);
    double taxa = iniciados == 0 ? 0 : Math.round(concluidos * 10_000.0 / iniciados) / 100.0;

    Map<String, AjudaConteudo> porCodigo = conteudoRepository.findAll().stream()
        .collect(Collectors.toMap(AjudaConteudo::getCodigo, Function.identity()));
    List<AjudaMetricaItemResponse> maisAcessados = eventoRepository.conteudosMaisAcessados(
            desde, TipoAjudaEvento.CONTEUDO_ABERTO, PageRequest.of(0, LIMITE_RANKING)).stream()
        .map(item -> metrica(item, porCodigo.containsKey((String) item[0])
            ? porCodigo.get((String) item[0]).getTitulo() : (String) item[0]))
        .toList();
    List<AjudaMetricaItemResponse> termos = eventoRepository.buscasFrequentes(
            desde, List.of(TipoAjudaEvento.BUSCA, TipoAjudaEvento.BUSCA_SEM_RESULTADO),
            PageRequest.of(0, LIMITE_RANKING)).stream()
        .map(item -> metrica(item, (String) item[0]))
        .toList();
    return new AjudaMetricasResponse(desde, total, buscas, semResultado, iniciados, concluidos, taxa,
        maisAcessados, termos);
  }

  private AjudaConteudo buscar(UUID id) {
    return conteudoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Conteúdo de ajuda não encontrado."));
  }

  private void atualizar(AjudaConteudo conteudo, AjudaConteudoRequest request) {
    String jornadaCodigo = validarJornada(request);
    List<String> urls = request.mediaUrls() == null ? List.of() : request.mediaUrls().stream()
        .map(String::trim).filter(url -> !url.isBlank()).peek(this::validarUrlMedia).toList();
    TipoAjudaMedia tipoMedia = request.mediaTipo() == null ? TipoAjudaMedia.NENHUMA : request.mediaTipo();
    if (tipoMedia != TipoAjudaMedia.NENHUMA && urls.isEmpty()) {
      throw new BusinessException("Informe ao menos uma URL para a mídia selecionada.");
    }
    conteudo.atualizar(
        request.tipo(), jornadaCodigo, request.titulo().trim(),
        textoOpcional(request.resumo()), textoOpcional(request.conteudo()), textoOpcional(request.rotaContexto()),
        textoOpcional(request.rotaAcao()), textoOpcional(request.rotuloAcao()), textoOpcional(request.icone()),
        textoOpcional(request.seletorAlvo()), tipoMedia, urls.isEmpty() ? null : String.join("\n", urls),
        textoOpcional(request.mediaAlt()), request.ordem() == null ? 0 : request.ordem(),
        request.ativo() == null || request.ativo());
  }

  private String validarJornada(AjudaConteudoRequest request) {
    String jornadaCodigo = textoOpcional(request.jornadaCodigo());
    if (request.tipo() != TipoAjudaConteudo.ETAPA) {
      if (jornadaCodigo != null) {
        throw new BusinessException("Somente etapas podem possuir uma jornada vinculada.");
      }
      return null;
    }
    if (jornadaCodigo == null) {
      throw new BusinessException("Selecione a jornada à qual esta etapa pertence.");
    }
    String codigoNormalizado = jornadaCodigo.toUpperCase(Locale.ROOT);
    AjudaConteudo jornada = conteudoRepository.findByCodigo(codigoNormalizado)
        .orElseThrow(() -> new BusinessException("A jornada vinculada não existe."));
    if (jornada.getTipo() != TipoAjudaConteudo.JORNADA) {
      throw new BusinessException("O conteúdo vinculado precisa ser do tipo JORNADA.");
    }
    return codigoNormalizado;
  }

  private void validarUrlMedia(String valor) {
    try {
      URI uri = URI.create(valor);
      if (uri.isAbsolute() && !List.of("http", "https").contains(uri.getScheme().toLowerCase(Locale.ROOT))) {
        throw new BusinessException("A mídia deve usar uma URL HTTP, HTTPS ou um caminho interno.");
      }
      if (!uri.isAbsolute() && !valor.startsWith("/")) {
        throw new BusinessException("Caminhos internos de mídia devem começar com /.");
      }
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("URL de mídia inválida.");
    }
  }

  private boolean corresponde(AjudaConteudo item, String termo) {
    return normalizar(String.join(" ", item.getCodigo(), item.getTitulo(),
        item.getResumo() == null ? "" : item.getResumo(), item.getConteudo() == null ? "" : item.getConteudo()))
        .contains(termo);
  }

  private AjudaMetricaItemResponse metrica(Object[] item, String rotulo) {
    return new AjudaMetricaItemResponse((String) item[0], rotulo, ((Number) item[1]).longValue());
  }

  private String normalizar(String valor) {
    String texto = textoOpcional(valor);
    if (texto == null) return null;
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }

  private String textoOpcional(String valor) {
    if (valor == null || valor.isBlank()) return null;
    return valor.trim();
  }
}

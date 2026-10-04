package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.entity.ManualEvento;
import com.nexus.portal.docflow.repository.ManualEventoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** INT-605: registra o uso do manual hospedado, sem identificar o leitor. */
@Service
public class ManualEventoService {

  private static final int MAX_TERMO = 200;

  private final ManualLeitorService leitorService;
  private final ManualCorpusService corpusService;
  private final ManualEventoRepository repository;

  public ManualEventoService(
      ManualLeitorService leitorService, ManualCorpusService corpusService, ManualEventoRepository repository) {
    this.leitorService = leitorService;
    this.corpusService = corpusService;
    this.repository = repository;
  }

  @Transactional
  public void registrar(String token, String origem, ManualEvento.Tipo tipo, String termo, String codigoTela,
      Integer resultados) {
    UUID clienteId = leitorService.cliente(token, origem);
    UUID publicacaoId = corpusService.vigente(clienteId).publicacaoId();
    String termoLimpo = termo == null || termo.isBlank() ? null : truncar(termo.strip(), MAX_TERMO);
    if (tipo != ManualEvento.Tipo.PAGINA_ABERTA && termoLimpo == null) {
      return; // busca sem termo não diz nada
    }
    repository.save(new ManualEvento(clienteId, publicacaoId, tipo, termoLimpo,
        codigoTela == null || codigoTela.isBlank() ? null : truncar(codigoTela.strip(), 120), resultados));
  }

  private static String truncar(String texto, int max) {
    return texto.length() <= max ? texto : texto.substring(0, max);
  }
}

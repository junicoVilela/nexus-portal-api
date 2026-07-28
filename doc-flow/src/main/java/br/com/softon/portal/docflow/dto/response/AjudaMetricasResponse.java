package br.com.softon.portal.docflow.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record AjudaMetricasResponse(
    OffsetDateTime desde,
    long totalEventos,
    long buscas,
    long buscasSemResultado,
    long toursIniciados,
    long toursConcluidos,
    double taxaConclusaoTour,
    List<AjudaMetricaItemResponse> conteudosMaisAcessados,
    List<AjudaMetricaItemResponse> buscasFrequentes) {
}

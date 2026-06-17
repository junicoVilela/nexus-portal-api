package br.com.softon.portal.docflow.dto.response;

public record DownloadTokenResponse(String token, long validadeSegundos, String urlPath) {
}

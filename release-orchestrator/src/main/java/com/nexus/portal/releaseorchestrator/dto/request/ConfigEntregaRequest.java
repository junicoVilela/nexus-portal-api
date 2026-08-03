package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfigEntregaRequest(
    @NotNull TipoDestinoEntrega tipoDestino,
    @Size(max = 500) String caminhoBase,
    Boolean exigirAprovacao,
    @Size(max = 1000) String emailsNotificacao,
    /* --- Destino remoto (FTP/SFTP) --- */
    @Size(max = 200) String host,
    @Min(1) @Max(65535) Integer porta,
    @Size(max = 120) String usuario,
    /** Senha em texto plano. Enviar em branco no PUT preserva a senha atual. */
    @Size(max = 500) String senha,
    Boolean modoPassivo,
    Boolean strictHostCheck,
    /* --- Destino BUCKET (S3/MinIO) --- */
    @Size(max = 200) String bucket,
    @Size(max = 500) String endpoint,
    @Size(max = 60) String regiao,
    Boolean pathStyleAccess) {}

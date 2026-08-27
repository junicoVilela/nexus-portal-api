package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import org.springframework.stereotype.Component;

/**
 * Stub: registra o contrato de implantação sem falar com Docker, SSH ou WinRM.
 */
@Component
public class DryRunImplantacaoAdapter implements ImplantacaoAdapter {

  @Override
  public Resultado aplicar(Contexto contexto) {
    String host = contexto.host().getHostname();
    if (contexto.operacao() == OperacaoDeploy.INICIAR || contexto.operacao() == OperacaoDeploy.PARAR) {
      boolean iniciar = contexto.operacao() == OperacaoDeploy.INICIAR;
      if (contexto.instalacao().getTipoImplantacao() == TipoImplantacao.LINUX_MANUAL) {
        String dir = contexto.manifesto().diretorioInstalacao();
        String script = iniciar ? "start.sh" : "stop.sh";
        return Resultado.ok("Dry-run: não alterou o host " + host + ". "
            + "Rodaria scripts/" + script + " em " + (dir == null ? "?" : dir) + ".");
      }
      String nome = DockerPullImplantacaoAdapter.nomeContainer(contexto.instalacao().getCodigo());
      String acao = iniciar ? "iniciaria o container " : "pararia o container ";
      return Resultado.ok("Dry-run: não alterou o host " + host + ". " + acao + nome + ".");
    }
    String acao = contexto.operacao() == OperacaoDeploy.CRIAR
        ? "criaria o serviço (instalação ainda inexistente no host)"
        : "atualizaria o serviço já existente";
    if (contexto.instalacao().getTipoImplantacao() == TipoImplantacao.LINUX_MANUAL) {
      String dir = contexto.manifesto().diretorioInstalacao();
      String mensagem = "Dry-run: não alterou o host " + host + ". "
          + "Operação prevista: " + acao
          + " na pasta " + (dir == null ? "?" : dir)
          + ", gravando .env (Oracle/SQL Server) e rodando scripts/install.sh"
          + " (baixa JDK 8+17 e Tomcat se ainda não existirem). "
          + contexto.manifesto().resumo() + ".";
      return Resultado.ok(mensagem);
    }
    String mensagem = "Dry-run: não alterou o host " + host + ". "
        + "Operação prevista: " + acao + ". "
        + contexto.manifesto().resumo() + ".";
    return Resultado.ok(mensagem);
  }
}

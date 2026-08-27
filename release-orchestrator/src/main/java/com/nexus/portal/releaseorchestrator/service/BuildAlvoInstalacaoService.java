package com.nexus.portal.releaseorchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse.AlvoBuild;
import com.nexus.portal.releaseorchestrator.dto.response.FontesVersaoInstalacaoResponse.BuildArtefatoStatus;
import com.nexus.portal.releaseorchestrator.entity.BuildInstalacaoArtefato;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.StatusBuildInstalacao;
import com.nexus.portal.releaseorchestrator.repository.BuildInstalacaoArtefatoRepository;
import com.nexus.portal.releaseorchestrator.repository.ModuloProdutoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta a lista de jobs/módulos que a ficha da instalação pode disparar. */
@Service
@RequiredArgsConstructor
public class BuildAlvoInstalacaoService {

  private final ModuloProdutoRepository moduloRepository;
  private final BuildInstalacaoArtefatoRepository buildRepository;
  private final ObjectMapper objectMapper;

  public List<AlvoBuild> listar(InstalacaoCliente inst) {
    ProdutoRh produto = inst.getProduto();
    List<AlvoBuild> alvos = new ArrayList<>();
    List<ModuloProduto> modulos = moduloRepository
        .findByProduto_IdAndAtivoTrueOrderByOrdemAscNomeAsc(produto.getId())
        .stream()
        .filter(m -> m.getTipo().aceitaUploadDeArtefato())
        .toList();
    if (modulos.isEmpty()) {
      if (produto.temIntegracaoJenkins()) {
        alvos.add(alvoProduto(produto, true));
      }
    } else {
      for (ModuloProduto m : modulos) {
        alvos.add(alvoModulo(produto, m, true));
      }
    }
    return alvos;
  }

  public List<BuildArtefatoStatus> recentes(UUID instalacaoId) {
    return buildRepository.findTop8ByInstalacao_IdOrderByCreatedAtDesc(instalacaoId).stream()
        .map(b -> new BuildArtefatoStatus(
            b.getId(),
            b.getAlvoId(),
            b.getProduto().getSigla(),
            b.getJenkinsJob(),
            b.getStatus().name(),
            b.getNomeArquivo(),
            b.getMensagem(),
            b.getCreatedAt()))
        .toList();
  }

  public List<AlvoBuild> selecionar(List<AlvoBuild> todos, List<String> alvoIds, UUID produtoInstalacaoId) {
    if (alvoIds == null || alvoIds.isEmpty()) {
      return todos.stream().filter(AlvoBuild::doProdutoDaInstalacao).toList();
    }
    return todos.stream().filter(a -> alvoIds.contains(a.id())).toList();
  }

  @Transactional(readOnly = true)
  public List<BuildInstalacaoArtefato> carregarParaAcompanhar(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return buildRepository.findByIdIn(ids);
  }

  @Transactional
  public void atualizarAcompanhamento(UUID id, Integer buildNumber, StatusBuildInstalacao status, String mensagem) {
    buildRepository.findById(id).ifPresent(row -> {
      if (buildNumber != null) {
        row.marcarNumero(buildNumber);
      }
      if (status != null) {
        row.concluir(status, mensagem);
      }
      buildRepository.save(row);
    });
  }

  private AlvoBuild alvoProduto(ProdutoRh produto, boolean daInstalacao) {
    return new AlvoBuild(
        "produto:" + produto.getId(),
        produto.getSigla().toLowerCase(),
        "Job " + produto.getSigla() + " — " + (produto.getJenkinsJob() == null ? "—" : produto.getJenkinsJob()),
        "PRODUTO",
        produto.getId(),
        produto.getSigla(),
        produto.getJenkinsJob(),
        null,
        "*.war,*.jar,*.ear",
        daInstalacao,
        daInstalacao);
  }

  private AlvoBuild alvoModulo(ProdutoRh produto, ModuloProduto modulo, boolean daInstalacao) {
    ConfigEmpacotamentoModulo cfg = ConfigEmpacotamentoModulo.de(modulo.getConfigEspecifica(), objectMapper);
    String job = cfg.jobOu(produto.getJenkinsJob());
    boolean temJob = job != null && !job.isBlank();
    return new AlvoBuild(
        "modulo:" + modulo.getId(),
        modulo.getCodigo(),
        modulo.getNome(),
        "MODULO",
        produto.getId(),
        produto.getSigla(),
        job,
        cfg.nomeDestino(),
        cfg.padraoAssetOuDefault(modulo.getTipo()),
        daInstalacao,
        daInstalacao && cfg.marcadoPadrao(temJob));
  }
}

/*
 * Nexus Manual — help-bridge (Onda D). Abre a ajuda da tela atual e pergunta ao manual vigente.
 *
 *   <script src="https://<api>/api/v1/manual/help-bridge.js" data-token="nxm_..."></script>
 *   NexusManual.open({ codigoTela: 'PED-001' });
 *   const r = await NexusManual.perguntar('como filtrar pedidos?');
 *
 * Manual local (ZIP descompactado, file://): NexusManual.configure({ localUrl: 'file:///.../manual/index.html' }).
 */
(function () {
  'use strict';
  var script = document.currentScript;
  var src = script && script.src ? script.src : '';
  var config = {
    // .../api/v1/manual/help-bridge.js → .../api/v1
    apiBase: (script && script.dataset.apiBase) || src.replace(/\/manual\/help-bridge\.js(\?.*)?$/, ''),
    token: (script && script.dataset.token) || null,
    localUrl: (script && script.dataset.localUrl) || null,
    alvo: (script && script.dataset.alvo) || 'nexus-manual',
  };

  function exigirToken() {
    if (!config.token) throw new Error('NexusManual: informe a chave do manual (data-token ou configure({ token })).');
  }

  function base() {
    exigirToken();
    return config.apiBase + '/manual/' + encodeURIComponent(config.token);
  }

  /** URL da tela no manual: local (ZIP) ou hospedado (manual vigente do cliente). */
  function urlTela(codigoTela) {
    var tela = codigoTela ? '?tela=' + encodeURIComponent(codigoTela) : '';
    if (config.localUrl) return config.localUrl + tela;
    return base() + '/site/index.html' + tela;
  }

  async function json(resposta) {
    var corpo = await resposta.json().catch(function () { return {}; });
    if (!resposta.ok) {
      var erro = new Error(corpo.message || corpo.mensagem || 'Manual indisponível (' + resposta.status + ').');
      erro.status = resposta.status;
      throw erro;
    }
    return corpo;
  }

  window.NexusManual = {
    configure: function (opcoes) {
      Object.assign(config, opcoes || {});
      return this;
    },
    /** Abre a página da tela; código desconhecido abre o manual com um aviso. */
    open: function (opcoes) {
      var codigoTela = opcoes && opcoes.codigoTela;
      return window.open(urlTela(codigoTela), (opcoes && opcoes.alvo) || config.alvo);
    },
    /** { codigoTela, titulo, caminho, versao, url } ou null se a tela não estiver no manual. */
    tela: async function (codigoTela) {
      var resposta = await fetch(base() + '/tela/' + encodeURIComponent(codigoTela));
      if (resposta.status === 404) return null;
      return json(resposta);
    },
    /** { modo: 'IA' | 'TRECHOS' | 'NAO_SEI', resposta, citacoes[] } — só com o manual publicado. */
    perguntar: async function (pergunta) {
      exigirToken();
      var resposta = await fetch(config.apiBase + '/ai/manual/' + encodeURIComponent(config.token) + '/perguntar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pergunta: pergunta }),
      });
      return json(resposta);
    },
    urlTela: urlTela,
  };
})();

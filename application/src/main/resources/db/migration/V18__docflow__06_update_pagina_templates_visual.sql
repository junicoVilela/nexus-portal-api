UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Visão geral</span>
  <h2>Objetivo da funcionalidade</h2>
  <p>Explique de forma direta o que esta funcionalidade permite realizar e qual problema ela resolve.</p>
</section>
<section class="doc-section doc-section--soft">
  <h2>Antes de começar</h2>
  <ul class="checklist">
    <li>Informe os acessos necessários.</li>
    <li>Liste as configurações ou dados que o usuário deve preparar.</li>
  </ul>
</section>
<section class="doc-section steps">
  <h2>Como utilizar</h2>
  <ol>
    <li>Acesse a funcionalidade.</li>
    <li>Preencha as informações necessárias.</li>
    <li>Revise os dados e confirme a operação.</li>
  </ol>
</section>
<div class="result-card">
  <strong>Resultado esperado</strong>
  <p>Descreva o resultado apresentado ao usuário após concluir o procedimento.</p>
</div>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'FUNCIONALIDADE';

UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Guia operacional</span>
  <h2>O que você vai fazer</h2>
  <p>Resuma o procedimento e informe o tempo ou resultado esperado.</p>
</section>
<section class="doc-section doc-section--soft">
  <h2>Antes de começar</h2>
  <ul class="checklist">
    <li>Separe as informações necessárias.</li>
    <li>Confirme que você possui as permissões adequadas.</li>
  </ul>
</section>
<section class="doc-section steps">
  <h2>Passo a passo</h2>
  <ol>
    <li>Acesse a tela indicada.</li>
    <li>Localize a opção desejada.</li>
    <li>Preencha os campos obrigatórios.</li>
    <li>Revise as informações e confirme a operação.</li>
  </ol>
</section>
<div class="result-card">
  <strong>Como validar</strong>
  <p>Explique como confirmar visualmente que o procedimento foi concluído com sucesso.</p>
</div>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'PASSO_A_PASSO';

UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Cadastro e edição</span>
  <h2>Quando utilizar</h2>
  <p>Descreva em quais situações o usuário deve acessar este cadastro e o que será registrado.</p>
</section>
<section class="doc-section">
  <h2>Campos da tela</h2>
  <p>Use a tabela abaixo para explicar o preenchimento de cada informação.</p>
  <div class="table-wrap">
    <table>
      <thead>
        <tr><th>Campo</th><th>Obrigatório</th><th>Como preencher</th></tr>
      </thead>
      <tbody>
        <tr><td>Nome do campo</td><td>Sim</td><td>Explique o formato e a regra de preenchimento.</td></tr>
      </tbody>
    </table>
  </div>
</section>
<section class="doc-section steps">
  <h2>Como cadastrar</h2>
  <ol>
    <li>Acesse a tela de cadastro.</li>
    <li>Preencha os campos conforme as orientações.</li>
    <li>Revise os dados e selecione <strong>Salvar</strong>.</li>
  </ol>
</section>
<div class="callout">
  <strong>Dica</strong>
  <p>Registre aqui uma orientação que ajude o usuário a evitar erros comuns.</p>
</div>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'CADASTRO';

UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Consulta e listagem</span>
  <h2>Visão geral</h2>
  <p>Explique quais informações esta consulta apresenta e em que situações ela é útil.</p>
</section>
<section class="doc-section">
  <h2>Filtros disponíveis</h2>
  <div class="table-wrap">
    <table>
      <thead>
        <tr><th>Filtro</th><th>Descrição</th><th>Exemplo</th></tr>
      </thead>
      <tbody>
        <tr><td>Nome do filtro</td><td>Explique o critério de pesquisa.</td><td>Informe um valor de exemplo.</td></tr>
      </tbody>
    </table>
  </div>
</section>
<section class="doc-section doc-section--soft">
  <h2>Como interpretar os resultados</h2>
  <p>Descreva as principais colunas, a ordenação padrão e os estados apresentados na listagem.</p>
</section>
<section class="doc-section">
  <h2>Ações disponíveis</h2>
  <ul class="checklist">
    <li>Visualizar os detalhes de um registro.</li>
    <li>Editar uma informação existente.</li>
    <li>Exportar ou compartilhar os resultados.</li>
  </ul>
</section>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'CONSULTA';

UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Central de dúvidas</span>
  <h2>Perguntas frequentes</h2>
  <p>Reúna respostas curtas para as dúvidas que mais chegam ao time de atendimento.</p>
</section>
<section class="doc-section faq">
  <div class="faq-list">
    <article class="faq-item">
      <h3>Quando devo utilizar esta funcionalidade?</h3>
      <p>Escreva uma resposta objetiva e indique o cenário recomendado.</p>
    </article>
    <article class="faq-item">
      <h3>O que fazer quando a operação não for concluída?</h3>
      <p>Indique as verificações iniciais e quando o canal de suporte deve ser acionado.</p>
    </article>
    <article class="faq-item">
      <h3>Posso desfazer esta ação?</h3>
      <p>Explique se a operação é reversível e quais cuidados o usuário deve tomar.</p>
    </article>
  </div>
</section>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'FAQ';

UPDATE tb_pagina_template
SET conteudo_html = $html$
<section class="doc-intro">
  <span class="doc-kicker">Diagnóstico</span>
  <h2>Identifique o problema</h2>
  <p>Descreva o sintoma observado, a mensagem exibida e em qual etapa o erro acontece.</p>
</section>
<section class="doc-section">
  <h2>Possíveis causas</h2>
  <ul>
    <li>Liste uma causa provável e como identificá-la.</li>
    <li>Inclua outra causa quando houver mais de um cenário.</li>
  </ul>
</section>
<section class="doc-section steps">
  <h2>Como resolver</h2>
  <ol>
    <li>Realize a primeira verificação.</li>
    <li>Aplique a correção recomendada.</li>
    <li>Repita a operação e valide o resultado.</li>
  </ol>
</section>
<div class="warning">
  <strong>Quando acionar o suporte</strong>
  <p>Informe os sinais de que o problema exige atendimento especializado e quais evidências devem ser enviadas.</p>
</div>
$html$,
    updated_at = now(),
    updated_by = 'migration'
WHERE codigo = 'SOLUCAO_PROBLEMAS';

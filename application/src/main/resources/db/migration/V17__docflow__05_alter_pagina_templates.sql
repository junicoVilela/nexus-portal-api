CREATE TABLE tb_pagina_template (
  id            UUID         NOT NULL DEFAULT gen_random_uuid(),
  codigo        VARCHAR(60)  NOT NULL,
  nome          VARCHAR(120) NOT NULL,
  descricao     VARCHAR(300),
  conteudo_html TEXT         NOT NULL,
  ordem         INT          NOT NULL DEFAULT 0,
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by    VARCHAR(120),
  updated_by    VARCHAR(120),
  CONSTRAINT pk_tb_pagina_template PRIMARY KEY (id),
  CONSTRAINT uq_tb_pagina_template_codigo UNIQUE (codigo)
);

CREATE INDEX idx_tb_pagina_template_ativo_ordem ON tb_pagina_template (ativo, ordem);

INSERT INTO tb_pagina_template
  (id, codigo, nome, descricao, conteudo_html, ordem, ativo, created_by, updated_by)
VALUES
  (
    '10000000-0000-0000-0000-000000000001',
    'FUNCIONALIDADE',
    'Página de funcionalidade',
    'Apresenta objetivo, pré-requisitos, utilização e resultado esperado.',
    '<h2>Objetivo</h2><p>Explique o que esta funcionalidade permite realizar.</p><h2>Pré-requisitos</h2><ul><li>Informe os acessos ou configurações necessários.</li></ul><h2>Como utilizar</h2><ol><li>Acesse a funcionalidade.</li><li>Preencha as informações necessárias.</li><li>Confirme a operação.</li></ol><h2>Resultado esperado</h2><p>Descreva o resultado apresentado ao usuário.</p>',
    10,
    TRUE,
    'migration',
    'migration'
  ),
  (
    '10000000-0000-0000-0000-000000000002',
    'PASSO_A_PASSO',
    'Passo a passo',
    'Guia operacional com preparação, etapas numeradas e validação final.',
    '<h2>Antes de começar</h2><p>Liste o que o usuário precisa preparar.</p><section class="steps"><h2>Passo a passo</h2><ol><li>Acesse a tela.</li><li>Localize a opção desejada.</li><li>Preencha os campos obrigatórios.</li><li>Confirme a operação.</li></ol></section><h2>Como validar</h2><p>Explique como confirmar que o procedimento funcionou.</p>',
    20,
    TRUE,
    'migration',
    'migration'
  ),
  (
    '10000000-0000-0000-0000-000000000003',
    'CADASTRO',
    'Cadastro ou edição',
    'Documenta campos, regras e ações de uma tela de cadastro.',
    '<h2>Quando utilizar</h2><p>Descreva quando o usuário deve acessar este cadastro.</p><h2>Campos</h2><table><thead><tr><th>Campo</th><th>Obrigatório</th><th>Descrição</th></tr></thead><tbody><tr><td>Nome do campo</td><td>Sim</td><td>Explique como preencher.</td></tr></tbody></table><h2>Como cadastrar</h2><ol><li>Acesse a tela.</li><li>Preencha os campos.</li><li>Selecione Salvar.</li></ol><div class="callout"><strong>Dica:</strong> registre uma orientação útil.</div>',
    30,
    TRUE,
    'migration',
    'migration'
  ),
  (
    '10000000-0000-0000-0000-000000000004',
    'CONSULTA',
    'Consulta ou listagem',
    'Explica filtros, resultados e ações disponíveis em uma listagem.',
    '<h2>Visão geral</h2><p>Explique quais informações esta consulta apresenta.</p><h2>Filtros disponíveis</h2><table><thead><tr><th>Filtro</th><th>Descrição</th></tr></thead><tbody><tr><td>Filtro</td><td>Explique o critério de pesquisa.</td></tr></tbody></table><h2>Resultados</h2><p>Descreva as colunas e a ordenação da listagem.</p><h2>Ações disponíveis</h2><ul><li>Visualizar</li><li>Editar</li><li>Exportar</li></ul>',
    40,
    TRUE,
    'migration',
    'migration'
  ),
  (
    '10000000-0000-0000-0000-000000000005',
    'FAQ',
    'Perguntas frequentes',
    'Organiza dúvidas recorrentes e respostas objetivas.',
    '<section class="faq"><h2>Perguntas frequentes</h2><h3>Quando devo utilizar esta funcionalidade?</h3><p>Escreva uma resposta objetiva.</p><h3>O que fazer quando a operação não for concluída?</h3><p>Indique as verificações e o canal de suporte.</p></section>',
    50,
    TRUE,
    'migration',
    'migration'
  ),
  (
    '10000000-0000-0000-0000-000000000006',
    'SOLUCAO_PROBLEMAS',
    'Solução de problemas',
    'Relaciona sintomas, possíveis causas e ações corretivas.',
    '<h2>Problema</h2><p>Descreva o sintoma observado pelo usuário.</p><h2>Possíveis causas</h2><ul><li>Liste uma causa provável.</li></ul><h2>Como resolver</h2><ol><li>Realize a primeira verificação.</li><li>Aplique a correção.</li><li>Repita a operação.</li></ol><div class="warning"><strong>Atenção:</strong> informe quando o suporte deve ser acionado.</div>',
    60,
    TRUE,
    'migration',
    'migration'
  )
ON CONFLICT (codigo) DO NOTHING;

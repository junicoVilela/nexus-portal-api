CREATE TABLE tb_ajuda_conteudo (
  id              UUID         NOT NULL DEFAULT gen_random_uuid(),
  codigo          VARCHAR(80)  NOT NULL,
  tipo            VARCHAR(30)  NOT NULL,
  jornada_codigo  VARCHAR(80),
  titulo          VARCHAR(160) NOT NULL,
  resumo          VARCHAR(400),
  conteudo        TEXT,
  rota_contexto   VARCHAR(220),
  rota_acao       VARCHAR(220),
  rotulo_acao     VARCHAR(80),
  icone           VARCHAR(50),
  seletor_alvo    VARCHAR(200),
  media_tipo      VARCHAR(20)  NOT NULL DEFAULT 'NENHUMA',
  media_urls      TEXT,
  media_alt       VARCHAR(240),
  ordem           INTEGER      NOT NULL DEFAULT 0,
  ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      VARCHAR(120),
  updated_by      VARCHAR(120),
  CONSTRAINT pk_tb_ajuda_conteudo PRIMARY KEY (id),
  CONSTRAINT uq_tb_ajuda_conteudo_codigo UNIQUE (codigo)
);

CREATE TABLE tb_ajuda_evento (
  id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
  tipo                 VARCHAR(30)  NOT NULL,
  conteudo_codigo      VARCHAR(80),
  termo                VARCHAR(240),
  rota                 VARCHAR(240),
  sessao_id            VARCHAR(80),
  resultado_quantidade INTEGER,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by           VARCHAR(120),
  CONSTRAINT pk_tb_ajuda_evento PRIMARY KEY (id)
);

CREATE INDEX idx_tb_ajuda_conteudo_tipo_ordem ON tb_ajuda_conteudo (tipo, ordem);
CREATE INDEX idx_tb_ajuda_conteudo_jornada ON tb_ajuda_conteudo (jornada_codigo, ordem);
CREATE INDEX idx_tb_ajuda_conteudo_rota ON tb_ajuda_conteudo (rota_contexto);
CREATE INDEX idx_tb_ajuda_evento_tipo_created ON tb_ajuda_evento (tipo, created_at DESC);
CREATE INDEX idx_tb_ajuda_evento_conteudo ON tb_ajuda_evento (conteudo_codigo, created_at DESC);

INSERT INTO tb_ajuda_conteudo
  (codigo, tipo, jornada_codigo, titulo, resumo, conteudo, rota_contexto, rota_acao,
   rotulo_acao, icone, seletor_alvo, ordem, created_by, updated_by)
VALUES
  ('JORNADA_ESTRUTURA', 'JORNADA', NULL, 'Preparar a estrutura', 'Cadastre a base que organiza todo o conteúdo do manual.', 'Cliente, projeto e módulo formam a estrutura do manual.', '/doc-flow', '/doc-flow/clientes/novo', 'Começar', 'Layers', NULL, 10, 'seed', 'seed'),
  ('ESTRUTURA_CLIENTE', 'ETAPA', 'JORNADA_ESTRUTURA', 'Cadastre o cliente', 'Defina quem receberá o manual.', 'O cliente concentra vínculos e publicações.', '/doc-flow/clientes', '/doc-flow/clientes/novo', 'Novo cliente', 'Building2', NULL, 11, 'seed', 'seed'),
  ('ESTRUTURA_PROJETO', 'ETAPA', 'JORNADA_ESTRUTURA', 'Crie um projeto', 'Represente o produto ou sistema documentado.', 'Projetos agrupam os módulos funcionais.', '/doc-flow/projetos', '/doc-flow/projetos/novo', 'Novo projeto', 'FolderOpen', NULL, 12, 'seed', 'seed'),
  ('ESTRUTURA_MODULO', 'ETAPA', 'JORNADA_ESTRUTURA', 'Organize os módulos', 'Separe funcionalidades em áreas fáceis de localizar.', 'Use nomes reconhecíveis pelos usuários do manual.', '/doc-flow/modulos', '/doc-flow/modulos/novo', 'Novo módulo', 'Layers', NULL, 13, 'seed', 'seed'),

  ('JORNADA_DOCUMENTAR', 'JORNADA', NULL, 'Criar uma página', 'Transforme conhecimento do produto em orientação clara.', 'Use modelos e blocos para acelerar a documentação.', '/doc-flow/paginas', '/doc-flow/paginas/novo', 'Criar página', 'FilePen', NULL, 20, 'seed', 'seed'),
  ('DOCUMENTAR_MODELO', 'ETAPA', 'JORNADA_DOCUMENTAR', 'Escolha um modelo', 'Comece por uma estrutura pronta para o tipo de conteúdo.', 'Selecione o modelo antes de preencher o conteúdo.', '/doc-flow/paginas', '/doc-flow/paginas/novo', 'Criar página', 'LayoutTemplate', NULL, 21, 'seed', 'seed'),
  ('DOCUMENTAR_CONTEXTO', 'ETAPA', 'JORNADA_DOCUMENTAR', 'Preencha contexto e conteúdo', 'Defina módulo, código, resumo e orientações.', 'Substitua todas as instruções temporárias do modelo.', '/doc-flow/paginas', '/doc-flow/paginas', 'Abrir páginas', 'FileText', NULL, 22, 'seed', 'seed'),
  ('DOCUMENTAR_QUALIDADE', 'ETAPA', 'JORNADA_DOCUMENTAR', 'Confira a qualidade', 'Resolva erros de imagens, links e campos obrigatórios.', 'O checklist bloqueia conteúdos incompletos antes da revisão.', '/doc-flow/paginas', '/doc-flow/paginas', 'Ver páginas', 'CheckCircle2', NULL, 23, 'seed', 'seed'),

  ('JORNADA_REVISAR', 'JORNADA', NULL, 'Revisar e aprovar', 'Valide o conteúdo com rastreabilidade.', 'A revisão registra decisões e mantém o histórico editorial.', '/doc-flow/revisoes', '/doc-flow/revisoes', 'Abrir revisões', 'ClipboardCheck', NULL, 30, 'seed', 'seed'),
  ('REVISAR_ENVIAR', 'ETAPA', 'JORNADA_REVISAR', 'Envie para revisão', 'Use Enviar para revisão quando não houver erro impeditivo.', 'A página sai do rascunho e entra na fila central.', '/doc-flow/paginas', '/doc-flow/paginas', 'Abrir páginas', 'ArrowUpRight', NULL, 31, 'seed', 'seed'),
  ('REVISAR_ANALISAR', 'ETAPA', 'JORNADA_REVISAR', 'Analise checklist e alterações', 'Compare versões e registre observações.', 'Assuma a análise para deixar clara a responsabilidade.', '/doc-flow/revisoes', '/doc-flow/revisoes', 'Central de revisão', 'Search', NULL, 32, 'seed', 'seed'),
  ('REVISAR_APROVAR', 'ETAPA', 'JORNADA_REVISAR', 'Aprove ou devolva', 'Aprove conteúdos prontos ou devolva para ajustes.', 'Toda decisão fica registrada no histórico.', '/doc-flow/revisoes', '/doc-flow/revisoes', 'Revisar agora', 'CheckCircle2', NULL, 33, 'seed', 'seed'),

  ('JORNADA_PUBLICAR', 'JORNADA', NULL, 'Gerar a publicação', 'Monte e acompanhe a versão final do manual.', 'A publicação reúne somente páginas elegíveis.', '/doc-flow/publicacoes', '/doc-flow/publicacoes/novo', 'Gerar pacote', 'CloudUpload', NULL, 40, 'seed', 'seed'),
  ('PUBLICAR_PAGINAS', 'ETAPA', 'JORNADA_PUBLICAR', 'Publique as páginas aprovadas', 'Somente páginas publicadas entram no pacote.', 'Confirme também os vínculos com o cliente.', '/doc-flow/paginas', '/doc-flow/paginas', 'Ver páginas', 'FileText', NULL, 41, 'seed', 'seed'),
  ('PUBLICAR_DIAGNOSTICO', 'ETAPA', 'JORNADA_PUBLICAR', 'Revise o diagnóstico', 'Confira páginas elegíveis, avisos e erros.', 'Corrija os impedimentos antes de iniciar a geração.', '/doc-flow/publicacoes', '/doc-flow/publicacoes/novo', 'Gerar pacote', 'ClipboardCheck', NULL, 42, 'seed', 'seed'),
  ('PUBLICAR_DISTRIBUIR', 'ETAPA', 'JORNADA_PUBLICAR', 'Acompanhe e distribua', 'Baixe ZIP ou PDF e copie o link temporário.', 'O detalhe informa o andamento e eventuais falhas.', '/doc-flow/publicacoes', '/doc-flow/publicacoes', 'Ver publicações', 'Download', NULL, 43, 'seed', 'seed'),

  ('FAQ_ORDEM', 'FAQ', NULL, 'Qual é a ordem correta dos cadastros?', 'Cliente, projeto, módulo e página. Depois revise, aprove, publique e gere o pacote.', NULL, '/doc-flow', NULL, NULL, 'HelpCircle', NULL, 101, 'seed', 'seed'),
  ('FAQ_REVISAO', 'FAQ', NULL, 'Por que não consigo enviar uma página para revisão?', 'Verifique no checklist conteúdo mínimo, placeholders, imagens, links e campos obrigatórios.', NULL, '/doc-flow/paginas', NULL, NULL, 'HelpCircle', NULL, 102, 'seed', 'seed'),
  ('FAQ_PACOTE', 'FAQ', NULL, 'Por que uma página não aparece no pacote?', 'Ela precisa estar publicada e vinculada ao cliente pelo projeto, módulo ou vínculo direto.', NULL, '/doc-flow/publicacoes', NULL, NULL, 'HelpCircle', NULL, 103, 'seed', 'seed'),
  ('FAQ_FALHA', 'FAQ', NULL, 'O que fazer quando uma publicação falha?', 'Abra o diagnóstico, faça o ajuste e use Reprocessar.', NULL, '/doc-flow/publicacoes', NULL, NULL, 'HelpCircle', NULL, 104, 'seed', 'seed'),

  ('TOUR_DASHBOARD', 'TOUR_PASSO', NULL, 'Acompanhe o trabalho', 'O dashboard mostra o que exige atenção e os atalhos principais.', NULL, NULL, '/doc-flow', 'Abrir dashboard', 'BarChart2', '[data-help-id="dashboard"]', 201, 'seed', 'seed'),
  ('TOUR_ESTRUTURA', 'TOUR_PASSO', NULL, 'Monte a estrutura', 'Comece por clientes, projetos e módulos.', NULL, NULL, '/doc-flow/clientes', 'Abrir clientes', 'Building2', '[data-help-id="clientes"]', 202, 'seed', 'seed'),
  ('TOUR_PAGINAS', 'TOUR_PASSO', NULL, 'Crie o conteúdo', 'Modelos e blocos aceleram a criação das páginas.', NULL, NULL, '/doc-flow/paginas', 'Abrir páginas', 'FileText', '[data-help-id="paginas"]', 203, 'seed', 'seed'),
  ('TOUR_REVISOES', 'TOUR_PASSO', NULL, 'Valide antes de publicar', 'A fila central reúne checklist, histórico e decisão.', NULL, NULL, '/doc-flow/revisoes', 'Abrir revisões', 'ClipboardCheck', '[data-help-id="revisoes"]', 204, 'seed', 'seed'),
  ('TOUR_PUBLICACOES', 'TOUR_PASSO', NULL, 'Gere e distribua', 'Acompanhe pacotes, downloads e reprocessamentos.', NULL, NULL, '/doc-flow/publicacoes', 'Abrir publicações', 'CloudUpload', '[data-help-id="publicacoes"]', 205, 'seed', 'seed'),
  ('TOUR_AJUDA', 'TOUR_PASSO', NULL, 'Volte quando precisar', 'A Central de Ajuda reúne jornadas, pesquisa e respostas rápidas.', NULL, NULL, '/doc-flow/ajuda', 'Abrir ajuda', 'HelpCircle', '[data-help-id="ajuda"]', 206, 'seed', 'seed')
ON CONFLICT (codigo) DO NOTHING;

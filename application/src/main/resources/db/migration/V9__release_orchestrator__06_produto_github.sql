-- F2.1 — campos GitHub no Produto.
-- Spec: docs/release-orchestrator/09-produtos-cadastro.md §4.2

ALTER TABLE tb_produto_rh
  ADD COLUMN repositorio_github VARCHAR(200),
  ADD COLUMN branch_padrao      VARCHAR(80)  DEFAULT 'main',
  ADD COLUMN padrao_tag         VARCHAR(200) DEFAULT '^v\d+\.\d+\.\d+$',
  ADD COLUMN github_token       VARCHAR(500);

-- Repositório em forma 'owner/repo'. Aceita URL completa; normalização no
-- service. Nulo = sem integração GitHub (MVP — usa upload manual).
COMMENT ON COLUMN tb_produto_rh.repositorio_github IS 'owner/repo no GitHub (ex.: softon/dtec-ld). Nulo desabilita integração.';
COMMENT ON COLUMN tb_produto_rh.branch_padrao IS 'Branch base para comparar tags. Default main.';
COMMENT ON COLUMN tb_produto_rh.padrao_tag IS 'Regex que tags válidas devem atender. Default ^v\d+\.\d+\.\d+$.';
COMMENT ON COLUMN tb_produto_rh.github_token IS 'PAT GitHub (escopo repo). MVP: texto plano. Pós-MVP: referência a credencial.';

-- Catálogo lab: módulos do instalador V5 (WARs + app.jar) e higiene do CR
-- (repo sem barra inicial, tags tipo V5.4.3 / v1.3.14, nome distinto do LD).

UPDATE public.tb_produto_rh
SET repositorio_github = regexp_replace(repositorio_github, '^/+', ''),
    updated_at = now()
WHERE repositorio_github ~ '^/+';

UPDATE public.tb_produto_rh
SET padrao_tag = '(?i)^(release/)?v?\d+(\.[0-9]+)+$',
    nome = CASE WHEN nome = 'Softon V5' THEN 'DTEC Risco' ELSE nome END,
    updated_at = now()
WHERE sigla = 'CR';

INSERT INTO public.tb_modulo_produto (
    produto_id, codigo, nome, tipo, gera_delta, obrigatorio, ordem, ativo, config_especifica
)
SELECT p.id, v.codigo, v.nome, v.tipo, v.gera_delta, v.obrigatorio, v.ordem, true, v.config
FROM public.tb_produto_rh p
JOIN (
    VALUES
        ('LD', 'ld-backend', 'Backend WAR', 'WEB', false, true, 10,
         '{"jenkinsJob":"ld-v5-build","padraoAsset":"ldv4.war","nomeArtefato":"ldv4.war"}'),
        ('LD', 'ld-frontend', 'Frontend WAR', 'WEB', false, true, 20,
         '{"jenkinsJob":"ld-v5-build","padraoAsset":"ldv4-frontend.war","nomeArtefato":"ldv4-frontend.war"}'),
        ('LD', 'ld-app', 'Aplicação (app.jar)', 'BATCH', false, false, 30,
         '{"jenkinsJob":"","padraoAsset":"dtec-application-*.jar,app.jar","nomeArtefato":"app.jar","selecionadoPadrao":false}'),
        ('CR', 'cr-web', 'Classificador de risco', 'WEB', false, true, 10,
         '{"jenkinsJob":"cr-build","padraoAsset":"dtec-class-risco.war","nomeArtefato":"dtec-class-risco.war"}')
) AS v(sigla, codigo, nome, tipo, gera_delta, obrigatorio, ordem, config)
    ON p.sigla = v.sigla
WHERE NOT EXISTS (
    SELECT 1
    FROM public.tb_modulo_produto m
    WHERE m.produto_id = p.id
      AND m.codigo = v.codigo
);

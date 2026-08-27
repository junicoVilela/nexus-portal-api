-- CR: mesmo padrão do LD (branch principal + tags/release da major),
-- com branch main-coso e versões a partir da 2.

UPDATE public.tb_produto_rh
SET branch_padrao = 'main-coso',
    padrao_tag = '(?i)^(release/)?v?2(\.[0-9]+)+$',
    jenkins_job = COALESCE(NULLIF(jenkins_job, ''), 'cr-build'),
    jenkins_trigger_mode = COALESCE(jenkins_trigger_mode, 'MANUAL'),
    updated_at = now()
WHERE sigla = 'CR';

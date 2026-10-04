-- INT-302: uma release publicada que cita o código da tela marca a página como possivelmente
-- desatualizada. A marca some quando a página é publicada de novo.

ALTER TABLE public.tb_pagina
  ADD COLUMN desatualizada_por character varying(200),
  ADD COLUMN desatualizada_em timestamp with time zone;

COMMENT ON COLUMN public.tb_pagina.desatualizada_por IS
  'Release que alterou a tela depois da última publicação da página (ex.: "Portal 1.5.0").';

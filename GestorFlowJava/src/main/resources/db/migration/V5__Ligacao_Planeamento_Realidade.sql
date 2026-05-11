-- 1. Ligar as faturas ao plano de origem (para o gráfico não contar a dobrar)
ALTER TABLE public.compras ADD COLUMN movimento_planeado_id int8 REFERENCES public.movimentos_planeados(id);
ALTER TABLE public.vendas ADD COLUMN movimento_planeado_id int8 REFERENCES public.movimentos_planeados(id);

-- 2. Adicionar memória ao botão (evita gerar duas rendas no mesmo mês)
ALTER TABLE public.movimentos_planeados ADD COLUMN data_ultimo_processamento date NULL;

-- 3. Adaptar o Planeamento ao rigor das faturas (Trocar campos soltos por IDs reais)
ALTER TABLE public.movimentos_planeados DROP COLUMN taxa_iva; -- Removemos o numeric solto
ALTER TABLE public.movimentos_planeados ADD COLUMN tx_iva_id int8 REFERENCES public.tx_iva(id);
ALTER TABLE public.movimentos_planeados ADD COLUMN artigo_id int8 REFERENCES public.artigos(id);

-- 🛡️ Se já tens dados, vamos associar um artigo "Genérico" ou "Serviços" para não quebrar NOT NULLs futuramente
-- UPDATE public.movimentos_planeados SET artigo_id = (SELECT id FROM artigos LIMIT 1), tx_iva_id = (SELECT id FROM tx_iva LIMIT 1);
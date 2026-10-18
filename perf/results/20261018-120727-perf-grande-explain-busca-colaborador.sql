\pset pager off
\timing off
-- Consulta de contagem da busca de colaboradores, como o Hibernate gera (texto em nome/e-mail, digitos no CPF)
prepare busca(text, text, text, text, text) as
select count(e.id) from employee e
where translate(lower(e.name), $4, $5) like $1 escape '\'
   or translate(lower(e.email), $4, $5) like $2 escape '\'
   or e.cpf like $3 escape '\';

\echo '=== A. termo seletivo, literal ==='
explain (analyze, buffers, costs off, summary on)
select count(e.id) from employee e
where translate(lower(e.name), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') like '%colaborador 66705560%' escape '\'
   or translate(lower(e.email), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') like '%colaborador 66705560%' escape '\'
   or e.cpf like '%66705560%' escape '\';

\echo '=== B. termo que casa com todos (colaborador), literal ==='
explain (analyze, buffers, costs off, summary on)
select count(e.id) from employee e
where translate(lower(e.name), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') like '%colaborador%' escape '\'
   or translate(lower(e.email), 'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn') like '%colaborador%' escape '\'
   or e.cpf like '%%' escape '\';

\echo '=== C. termo seletivo, plano generico (como apos varias execucoes do PreparedStatement) ==='
set plan_cache_mode = force_generic_plan;
explain (analyze, buffers, costs off, summary on)
execute busca('%colaborador 66705560%', '%colaborador 66705560%', '%66705560%',
              'áàâãäéèêëíìîïóòôõöúùûüçñ', 'aaaaaeeeeiiiiooooouuuucn');
reset plan_cache_mode;
deallocate busca;

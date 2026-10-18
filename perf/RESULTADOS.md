# Resultados de desempenho

Registro das medições do teste de carga (`perf/run.sh`) e da contagem de consultas (`QueryCountIntegrationTest`). Só valores medidos. Toda proposta sem medição é marcada como **hipótese**.

- Volumes dos cenários (`perf/scenarios.json`) são **nominais**, sem relação com produção.
- Compare apenas execuções com o mesmo alvo, cenário e máquina. O alvo `perf` (app e banco limitados a 2 CPUs e 1 GiB cada) é a referência para a comparação entre modos de thread.
- Cada execução tem o JSON completo em `perf/results/`, de onde vêm os números abaixo.

## Modelo de entrada

```
Execução       <data>-<alvo>-<cenario>-<modo>.json
Commit         <hash curto>[-dirty]
Alvo           perf | local
Cenário        pequeno | medio | grande
Threads        on | off
VUs / duração  <n> / <d>; pausa entre iterações <s>
Pool           <máximo de conexões>
Máquina        <CPU; núcleos; memória>

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem
detalhe
busca
escrita
```

## Contagem de consultas (SELECT por requisição)

### Antes das correções

`QueryCountIntegrationTest`, massa uniforme de 50 obras (H2, perfil `test`). SELECT por requisição.

```
caso	pequeno	grande
GET /api/projects (5 x 50)	42	402
GET /api/projects?q=obra (5 x 50)	42	402
GET /api/clients (5 x 50)	22	202
GET /api/clients?q=cliente (5 x 50)	22	202
GET /api/employees (5 x 50)	8	53
GET /api/employees?q=colaborador (5 x 50)	8	53
GET /api/cost-centers (5 x 50)	igual	igual
GET /api/accommodations (5 x 50)	22	202
GET /api/meals (5 x 50)	13	103
GET /api/restaurants (5 x 50)	24	204
GET /api/users (5 x 50)	7	52
GET /api/clients/{id} (1 x 10 obras)	4	13
POST /api/projects (2 x 30 colaboradores)	14	104
POST /api/accommodations (2 x 30 colaboradores)	22	162
GET /api/projects/{id}, /api/accommodations/{id}, /api/meals/{id}	igual	igual
```

### Depois das correções (LAZY + carga em lote, obras/clientes/colaboradores por IN, vínculos por findAllById)

```
caso	pequeno	grande
GET /api/projects (5 x 50)	15	15
GET /api/projects?q=obra (5 x 50)	15	15
GET /api/clients (5 x 50)	5	5
GET /api/clients?q=cliente (5 x 50)	5	5
GET /api/employees (5 x 50)	4	4
GET /api/employees?q=colaborador (5 x 50)	4	4
GET /api/cost-centers (5 x 50)	2	2
GET /api/accommodations (5 x 50)	10	10
GET /api/meals (5 x 50)	9	9
GET /api/restaurants (5 x 50)	15	15
GET /api/users (5 x 50)	3	3
GET /api/clients/{id} (1 x 10 obras)	4	4
GET /api/projects/{id} (2 x 30 colaboradores)	14	14
GET /api/accommodations/{id} (2 x 30 colaboradores, 1 x 4 períodos)	9	9
GET /api/meals/{id} (0 x 5 ajustes)	8	8
POST /api/projects (2 x 30 colaboradores)	14	14
POST /api/accommodations (2 x 30 colaboradores)	25	25
```

## Execuções

### Linha de base — antes das correções (código do commit `a36aa88`, sem virtual threads)

```
Execução       20261018-114132-perf-medio-off.json
Commit         a36aa88-dirty (imagem de 11:40:09; alterações locais só em perf/, testes, README e perfil local, nenhuma em código de produção)
Alvo           perf
Cenário        medio
Threads        off (o código ainda não tinha a chave; Tomcat com threads de plataforma)
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10 (padrão do Hikari)
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          16023 requisições, 131,2 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	7776	63,7	0,00	89,9	256,7	398,4
detalhe	4480	36,7	0,00	9,8	68,1	86,4
busca	2277	18,6	0,00	87,8	299,0	482,5
escrita	1466	12,0	0,00	23,2	89,9	287,6
```

### Depois das correções, threads de plataforma (1ª)

```
Execução       20261018-121330-perf-medio-off.json
Commit         a36aa88-dirty (código deste change, não commitado)
Alvo           perf
Cenário        medio
Threads        off
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          23540 requisições, 195,3 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	11232	93,2	0,00	67,1	106,5	188,0
detalhe	6717	55,7	0,00	13,0	85,9	97,8
busca	3395	28,2	0,00	80,4	170,2	202,3
escrita	2172	18,0	0,00	72,5	98,9	167,5
```
### Depois das correções, threads de plataforma (2ª)

```
Execução       20261018-122034-perf-medio-off.json
Commit         a36aa88-dirty (código deste change, não commitado)
Alvo           perf
Cenário        medio
Threads        off
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          22982 requisições, 190,6 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	10951	90,8	0,00	68,8	109,3	193,1
detalhe	6635	55,0	0,00	13,5	86,1	96,6
busca	3318	27,5	0,00	80,8	170,2	212,8
escrita	2054	17,0	0,00	74,9	101,8	175,4
```
### Depois das correções, virtual threads (1ª)

```
Execução       20261018-121638-perf-medio-on.json
Commit         a36aa88-dirty (código deste change, não commitado)
Alvo           perf
Cenário        medio
Threads        on
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          58743 requisições, 487,6 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	28098	233,2	0,00	14,5	61,7	86,8
detalhe	16510	137,0	0,00	11,1	52,4	74,1
busca	8551	71,0	0,00	18,5	65,9	92,2
escrita	5560	46,2	0,00	17,1	62,6	87,4
```
### Depois das correções, virtual threads (2ª, com JFR ativo)

```
Execução       20261018-122341-perf-medio-on.json
Commit         a36aa88-dirty (código deste change, não commitado)
Alvo           perf
Cenário        medio
Threads        on; JFR ligado (jdk.VirtualThreadPinned)
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          60613 requisições, 502,9 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	29005	240,7	0,00	14,6	60,1	85,9
detalhe	17202	142,7	0,00	11,0	50,2	73,0
busca	8734	72,5	0,00	18,0	63,6	88,7
escrita	5648	46,9	0,00	16,9	60,0	81,9
```
### Cenário grande, virtual threads (massa para a análise de índices)

```
Execução       20261018-120727-perf-grande-on.json
Commit         a36aa88-dirty (código deste change, não commitado)
Alvo           perf
Cenário        grande
Threads        on
VUs / duração  10 / 2m; pausa entre iterações 0 s
Pool           10
Máquina        Intel(R) Core(TM) i5-10210U CPU @ 1.60GHz; 8 CPUs; 8 GiB; app e banco limitados a 2 CPUs e 1 GiB cada
Total          33209 requisições, 275,6 req/s, falhas 0,00 %

grupo	requisições	req/s	falhas %	p50 (ms)	p95 (ms)	p99 (ms)
listagem	15662	130,0	0,00	14,3	77,5	91,4
detalhe	9499	78,8	0,00	8,5	66,2	76,1
busca	4768	39,6	0,00	88,9	297,6	369,9
escrita	3256	27,0	0,00	17,4	77,7	91,9
```
### Comparação (cenário medio, alvo perf, 10 VUs, 2 min)

```
execução	código	threads	req/s	p95 listagem (ms)	p95 detalhe (ms)	p95 busca (ms)	p95 escrita (ms)	falhas %
114132	antes	plataforma	131,2	256,7	68,1	299,0	89,9	0,00
121330	depois	plataforma	195,3	106,5	85,9	170,2	98,9	0,00
122034	depois	plataforma	190,6	109,3	86,1	170,2	101,8	0,00
121638	depois	virtual	487,6	61,7	52,4	65,9	62,6	0,00
122341	depois	virtual	502,9	60,1	50,2	63,6	60,0	0,00
```

- Mesmo modo de thread (plataforma), antes e depois das correções de N+1: vazão de 131,2 para 195,3 e 190,6 req/s; p95 das listagens de 256,7 para 106,5 e 109,3 ms.
- Mesmo código, plataforma × virtual: vazão de 195,3 e 190,6 para 487,6 e 502,9 req/s; p95 abaixo de 66 ms em todos os grupos. O resultado se repetiu nas duas rodadas de cada modo.
- No PostgreSQL, o custo médio por consulta é equivalente nos dois modos (`*-pg-top.txt`); a diferença está no lado da aplicação. **A causa não foi medida.** Hipótese a verificar: com o contêiner limitado a 2 CPUs, as threads de plataforma do Tomcat disputam a cota de CPU, enquanto as virtual threads rodam em 2 carrier threads.
- Pinning (JFR, 153 s de gravação, execução 122341): 1 evento `jdk.VirtualThreadPinned`, 40,3 ms, "Contended monitor enter" na inicialização de uma classe do Hibernate na primeira consulta. Sem ação.

## Análise de índices

Massa do cenário grande (10.000 colaboradores), PostgreSQL 16.10 do alvo perf, banco ocioso após a carga. A consulta de maior custo total na carga foi o `count` da busca `q` de colaboradores: 1.181 execuções, média 175,4 ms, 207 s no total (`20261018-120727-perf-grande-on-pg-top.txt`). Planos completos em `perf/results/20261018-120727-perf-grande-explain-busca-colaborador-*.txt`.

```
caso	sem índice	com índices trigram (GIN, pg_trgm) em translate(lower(name/email)) e cpf
A. termo seletivo, constantes literais	49,6 ms, seq scan	1,6 ms, bitmap index scan
B. termo que casa com todos (q=colaborador)	20,8 ms, seq scan	19,3 ms, seq scan (correto: o índice não ajuda)
C. termo seletivo, plano genérico (caminho real do PreparedStatement)	45,7 ms, seq scan	50,2 ms, seq scan (índice não usado)
```

- O índice só é usado quando as constantes do `translate` aparecem literais no SQL. O Hibernate as envia como parâmetros (`$4`, `$5`); no plano genérico, que o PostgreSQL adota após algumas execuções do mesmo comando, a expressão não casa com a do índice.
- Conclusão: criar só o índice **não** melhora a aplicação. O ganho medido em A depende também de mudar a geração do SQL da busca.
- Demais candidatos do design (D4): `refresh_token(expires_at)`, ordenações de obras por `createdAt`/`startDate`. Não apareceram no roteiro nem no top de custo; sem medição, não entram.

## Propostas de melhoria

1. **Manter virtual threads ligadas (padrão).** Medido: vazão de 190–195 para 488–503 req/s, p95 abaixo de 66 ms em todos os grupos, no mesmo código e cenário. A causa da diferença é **hipótese** (ver Comparação); vale confirmar em outra máquina e com mais VUs antes de tomá-la como regra.
2. **Correções de N+1 (já aplicadas).** Medido: listagem de obras de 402 para 15 consultas com página 50; vazão de 131,2 para 190–195 req/s com threads de plataforma.
3. **Busca `q` de colaboradores com trigram.** Medido: 49,6 → 1,6 ms com termo seletivo, desde que as constantes do `translate` sejam literais no SQL. Exige duas mudanças juntas: (a) gerar a normalização com constantes literais (função registrada no Hibernate); (b) migração só para PostgreSQL com `create extension pg_trgm` e índices GIN. **Decisão (usuário, 2026-10-18): só registrar.** Nenhuma migração neste change; fica para um change próprio, depois de confirmar se o usuário do banco de produção pode criar a extensão (sem permissão, a migração falharia e a aplicação não subiria).
4. **Termos pouco seletivos na busca** (ex.: `q=colaborador`, que casa com todos): seq scan de cerca de 20 ms com 10.000 linhas, com ou sem índice. **Hipótese**: exigir um tamanho mínimo de termo reduz esse custo; não medido.
5. **Tamanho do pool de conexões.** Não medido. **Hipótese**: com virtual threads e mais VUs, o pool de 10 passa a ser o limite; medir com `DB_POOL_MAX_SIZE` 10 × 20 e 50 VUs.
6. **Desligamento ordenado do contêiner (achado operacional, fora do escopo de desempenho).** Observado: o `Dockerfile` inicia a app com `sh -c "java ..."`, então o `SIGTERM` do `docker stop` não chega ao Java e o contêiner é morto por `SIGKILL` depois do prazo (o JFR não foi gravado por isso). Proposta: `exec java $JAVA_OPTS -jar app.jar` no `ENTRYPOINT`.

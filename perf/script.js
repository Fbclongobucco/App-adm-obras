// Roteiro de carga (spec performance-testing). Uso:
//   k6 run -e BASE_URL=... -e VUS=10 -e DURATION=2m -e RESULT_FILE=/results/x.json script.js
// Cada VU faz login uma vez (o BCrypt do login nao deve dominar a medicao) e repete um mix de
// listagens, detalhes, busca e uma escrita. Metricas separadas por grupo (tag "grupo").
import http from 'k6/http';
import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { BASE_URL, authHeaders, cnpj, login } from './lib.js';

const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '2m';
const THINK_TIME = Number(__ENV.THINK_TIME || 0);
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@admin.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin123';
const GROUPS = ['listagem', 'detalhe', 'busca', 'escrita'];

// O access token vale 15 min; acima disso o VU faz login de novo ao receber 401
export const options = {
  scenarios: {
    carga: { executor: 'constant-vus', vus: VUS, duration: DURATION },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(50)', 'p(95)', 'p(99)', 'max'],
  // Limites folgados: so existem para o k6 calcular as metricas de cada grupo no resumo
  thresholds: Object.fromEntries(GROUPS.flatMap((g) => [
    [`http_req_duration{grupo:${g}}`, ['max>=0']],
    [`http_req_failed{grupo:${g}}`, ['rate>=0']],
    [`http_reqs{grupo:${g}}`, ['count>=0']],
  ])),
};

const LISTINGS = ['projects', 'clients', 'employees', 'cost-centers', 'accommodations', 'meals', 'restaurants', 'users'];
const DETAILS = ['projects', 'clients', 'employees', 'accommodations', 'meals'];
const SEARCHES = [['projects', 'obra'], ['employees', 'colaborador'], ['clients', 'cliente'], ['employees', '123']];

export function setup() {
  const params = authHeaders(login(ADMIN_EMAIL, ADMIN_PASSWORD));
  const data = { pages: {}, ids: {} };
  for (const resource of LISTINGS) {
    const page = http.get(`${BASE_URL}/api/${resource}?size=20`, params).json();
    data.pages[resource] = Math.max(page.totalPages, 1);
  }
  for (const resource of DETAILS) {
    const page = http.get(`${BASE_URL}/api/${resource}?size=100`, params).json();
    data.ids[resource] = page.content.map((item) => item.id);
    if (data.ids[resource].length === 0) {
      exec.test.abort(`sem registros em /api/${resource}; crie a massa antes (seed.js)`);
    }
  }
  return data;
}

let token;

function call(method, path, body, grupo, name) {
  if (!token) {
    token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
  }
  const send = () => http.request(method, `${BASE_URL}${path}`, body ? JSON.stringify(body) : null,
    { ...authHeaders(token), tags: { grupo, name } });
  let response = send();
  if (response.status === 401) {
    token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    response = send();
  }
  check(response, { [`${grupo} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
  return response;
}

const any = (list) => list[Math.floor(Math.random() * list.length)];

export default function (data) {
  const roll = Math.random();
  if (roll < 0.5) {
    const resource = any(LISTINGS);
    const page = Math.floor(Math.random() * data.pages[resource]);
    call('GET', `/api/${resource}?page=${page}&size=20`, null, 'listagem', `GET /api/${resource}`);
  } else if (roll < 0.8) {
    const resource = any(DETAILS);
    call('GET', `/api/${resource}/${any(data.ids[resource])}`, null, 'detalhe', `GET /api/${resource}/{id}`);
  } else if (roll < 0.95) {
    const [resource, q] = any(SEARCHES);
    call('GET', `/api/${resource}?q=${q}&size=20`, null, 'busca', `GET /api/${resource}?q`);
  } else {
    // CNPJ unico por VU e iteracao; o centro e excluido em seguida para a massa nao crescer
    const n = (exec.vu.idInTest * 1e5 + exec.vu.iterationInScenario) % 1e8;
    const created = call('POST', '/api/cost-centers', { name: `Centro carga ${n}`, cnpj: cnpj(n) },
      'escrita', 'POST /api/cost-centers');
    if (created.status === 201) {
      call('DELETE', `/api/cost-centers/${created.json('id')}`, null, 'escrita', 'DELETE /api/cost-centers/{id}');
    }
  }
  if (THINK_TIME > 0) {
    sleep(THINK_TIME);
  }
}

function metric(data, name, group) {
  return data.metrics[`${name}{grupo:${group}}`]?.values || {};
}

export function handleSummary(data) {
  const meta = {
    target: __ENV.META_TARGET || 'desconhecido',
    scenario: __ENV.META_SCENARIO || 'desconhecido',
    threads: __ENV.META_THREADS || 'desconhecido',
    pool: __ENV.META_POOL || 'padrao',
    commit: __ENV.META_COMMIT || '',
    machine: __ENV.META_MACHINE || '',
    vus: VUS,
    duration: DURATION,
    thinkTime: THINK_TIME,
    baseUrl: BASE_URL,
  };
  const groups = Object.fromEntries(GROUPS.map((g) => {
    const duration = metric(data, 'http_req_duration', g);
    return [g, {
      requests: metric(data, 'http_reqs', g).count || 0,
      rate: metric(data, 'http_reqs', g).rate || 0,
      failed: metric(data, 'http_req_failed', g).rate || 0,
      p50: duration['p(50)'], p95: duration['p(95)'], p99: duration['p(99)'],
    }];
  }));
  const total = data.metrics.http_reqs.values;
  const lines = [
    `alvo=${meta.target} cenario=${meta.scenario} threads=${meta.threads} vus=${VUS} duracao=${DURATION}`,
    `total: ${total.count} requisicoes, ${total.rate.toFixed(1)} req/s, falhas ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`,
    'grupo      req     req/s   falha%   p50(ms)  p95(ms)  p99(ms)',
    ...GROUPS.map((g) => {
      const v = groups[g];
      const ms = (x) => (x === undefined ? '-' : x.toFixed(1)).padStart(8);
      return `${g.padEnd(9)}${String(v.requests).padStart(6)}${v.rate.toFixed(1).padStart(9)}`
        + `${(v.failed * 100).toFixed(2).padStart(8)} ${ms(v.p50)} ${ms(v.p95)} ${ms(v.p99)}`;
    }),
  ];
  const output = { stdout: `${lines.join('\n')}\n` };
  if (__ENV.RESULT_FILE) {
    output[__ENV.RESULT_FILE] = JSON.stringify({ meta, groups, k6: data }, null, 2);
  }
  return output;
}

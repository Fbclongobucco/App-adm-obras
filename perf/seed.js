// Massa de dados pela API (spec performance-testing). Uso:
//   k6 run -e SCENARIO=pequeno -e BASE_URL=... -e ADMIN_EMAIL=... -e ADMIN_PASSWORD=... seed.js
// SKIP_IF_SEEDED=true nao cria nada se ja existirem obras (alvo local, que preserva os dados).
import http from 'k6/http';
import exec from 'k6/execution';
import { BASE_URL, authHeaders, cnpj, cpf, login } from './lib.js';

const SCENARIOS = JSON.parse(open('./scenarios.json'));
const SCENARIO_NAME = __ENV.SCENARIO || 'pequeno';
const SCENARIO = SCENARIOS[SCENARIO_NAME];
// Cenario inexistente falha aqui, na inicializacao, antes de qualquer requisicao
if (!SCENARIO || SCENARIO_NAME.startsWith('_')) {
  throw new Error(`cenario '${SCENARIO_NAME}' nao existe em scenarios.json; use: `
    + Object.keys(SCENARIOS).filter((k) => !k.startsWith('_')).join(', '));
}

const BATCH = Number(__ENV.SEED_BATCH || 20);

export const options = {
  scenarios: {
    // A massa grande leva minutos; o padrao do k6 (10 min) seria pouco
    seed: { executor: 'shared-iterations', vus: 1, iterations: 1, maxDuration: __ENV.SEED_MAX_DURATION || '2h' },
  },
  batchPerHost: BATCH,
  summaryTrendStats: ['avg', 'p(95)'],
};

// Unicidade entre execucoes: OS, e-mails, CPF e CNPJ usam o instante da execucao como prefixo
const RUN = Date.now() % 1e4;
let sequence = 0;
const next = () => RUN * 1e5 + (sequence++);

const ADDRESS = {
  street: 'Rua das Palmeiras', number: '120', city: 'Campinas', state: 'SP',
  country: 'Brasil', neighborhood: 'Centro', zipCode: '13010-000',
};

/** POST em lotes concorrentes; para no primeiro cadastro rejeitado. Devolve os ids na ordem. */
function createAll(params, path, bodies) {
  const ids = [];
  for (let i = 0; i < bodies.length; i += BATCH) {
    const chunk = bodies.slice(i, i + BATCH);
    const responses = http.batch(chunk.map((body) => ['POST', `${BASE_URL}${path}`, JSON.stringify(body), params]));
    responses.forEach((response, j) => {
      if (response.status !== 201) {
        exec.test.abort(`POST ${path} rejeitado (${response.status}): ${response.body}\ncorpo: ${JSON.stringify(chunk[j])}`);
      }
      ids.push(response.json('id'));
    });
  }
  console.log(`${path}: ${ids.length}`);
  return ids;
}

const pick = (list, i) => list[i % list.length];

function slice(list, start, count) {
  return Array.from({ length: Math.min(count, list.length) }, (_, k) => pick(list, start + k));
}

export default function () {
  const token = login(__ENV.ADMIN_EMAIL || 'admin@admin.com', __ENV.ADMIN_PASSWORD || 'admin123');
  const params = authHeaders(token);

  if (__ENV.SKIP_IF_SEEDED === 'true') {
    const existing = http.get(`${BASE_URL}/api/projects?size=1`, params).json('totalElements');
    if (existing > 0) {
      console.log(`base ja tem ${existing} obras; massa nao criada (SKIP_IF_SEEDED)`);
      return;
    }
  }
  const s = SCENARIO;
  console.log(`cenario ${SCENARIO_NAME} em ${BASE_URL}`);

  const costCenters = createAll(params, '/api/cost-centers', Array.from({ length: s.costCenters }, () => {
    const n = next();
    return { name: `Centro ${n}`, cnpj: cnpj(n) };
  }));

  const clients = createAll(params, '/api/clients', Array.from({ length: s.clients }, () => {
    const n = next();
    return { name: `Cliente ${n}`, email: `cliente${n}@carga.test`, phone: '1933334444', address: ADDRESS };
  }));

  const employees = createAll(params, '/api/employees', Array.from({ length: s.employees }, (_, i) => {
    const n = next();
    return {
      name: `Colaborador ${n}`, email: `colaborador${n}@carga.test`, cpf: cpf(n), phone: '19988887777',
      birthDate: '1990-05-10', costCenterId: pick(costCenters, i), role: 'MONTADOR',
    };
  }));

  const projects = createAll(params, '/api/projects', Array.from({ length: s.projects }, (_, i) => ({
    os: `OS-${next()}`, description: `Obra de carga ${i}`, costCenterId: pick(costCenters, i),
    clientId: pick(clients, i), startDate: '2026-01-05', isCompleted: false,
    employeeIds: slice(employees, i * s.employeesPerProject, s.employeesPerProject),
  })));

  // Almoco em todas as obras; jantar nas obras pares, em outro restaurante
  const lunchRestaurants = createAll(params, '/api/restaurants', projects.map((projectId) => ({
    name: `Restaurante ${next()}`, phone: '1933336666', address: ADDRESS, projectId,
  })));
  const dinnerProjects = projects.filter((_, i) => i % 2 === 0);
  const dinnerRestaurants = createAll(params, '/api/restaurants', dinnerProjects.map((projectId) => ({
    name: `Restaurante ${next()}`, phone: '1933336666', address: ADDRESS, projectId,
  })));
  const meal = (restaurantId, mealType) => ({
    restaurantId, mealType, isBilled: false,
    lunchQuantity: 10, lunchPrice: 25, dinnerQuantity: 8, dinnerPrice: 22,
  });
  createAll(params, '/api/meals', lunchRestaurants.map((id) => meal(id, 'LUNCH')));
  createAll(params, '/api/meals', dinnerRestaurants.map((id) => meal(id, 'DINNER')));

  createAll(params, '/api/accommodations', Array.from({ length: s.accommodations }, (_, i) => ({
    hostName: `Pousada ${next()}`, hostPhone: '1933335555', address: ADDRESS,
    capacity: s.employeesPerAccommodation + 1, isContract: true, projectId: pick(projects, i),
    employeeIds: slice(employees, i * s.employeesPerAccommodation, s.employeesPerAccommodation),
    startDate: '2026-10-12', monthlyPrice: 1500,
  })));

  createAll(params, '/api/users', Array.from({ length: s.users }, () => {
    const n = next();
    return { name: `Operador ${n}`, email: `operador${n}@carga.test`, isActive: true, roles: ['OPERADOR'],
      password: 'senha-forte-123' };
  }));
}

export function handleSummary() {
  return {};
}

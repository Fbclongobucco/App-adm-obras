// Funcoes comuns da massa (seed.js) e do roteiro de carga (script.js).
import http from 'k6/http';
import exec from 'k6/execution';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/** Login pelo fluxo real; devolve o access token ou aborta o teste. */
export function login(email, password) {
  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, password }),
    { headers: JSON_HEADERS, tags: { grupo: 'login' } });
  if (response.status !== 200) {
    exec.test.abort(`login de ${email} falhou: ${response.status} ${response.body}`);
  }
  return response.json('accessToken');
}

export function authHeaders(token) {
  return { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } };
}

// --- documentos validos (mesmo algoritmo de support/FakeDocuments) ---

const CPF_WEIGHTS = [[10, 9, 8, 7, 6, 5, 4, 3, 2], [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]];
const CNPJ_WEIGHTS = [[5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2], [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]];

function withCheckDigits(base, weights) {
  const digits = base.slice();
  for (const w of weights) {
    const sum = w.reduce((acc, weight, i) => acc + digits[i] * weight, 0);
    const remainder = sum % 11;
    digits.push(remainder < 2 ? 0 : 11 - remainder);
  }
  return digits.join('');
}

function digitsOf(value, length) {
  return String(value).padStart(length, '0').slice(-length).split('').map(Number);
}

/** CPF valido e unico por sequencia (nao aleatorio: 10.000 sorteios colidiriam). */
export function cpf(sequence) {
  let base = digitsOf(sequence, 9);
  if (base.every((d) => d === base[0])) {
    base = digitsOf(sequence + 1, 9);
  }
  return withCheckDigits(base, CPF_WEIGHTS);
}

/** CNPJ de matriz (filial 0001) valido e unico por sequencia. */
export function cnpj(sequence) {
  return withCheckDigits([...digitsOf(sequence, 8), 0, 0, 0, 1], CNPJ_WEIGHTS);
}

const BASE_URL = 'http://localhost:8081/api'

// Configurações para garantir UTF-8
const fetchOptions = {
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
    'Accept': 'application/json; charset=UTF-8'
  }
}

// Função utilitária para corrigir encoding se necessário
function fixEncoding(text) {
  if (!text || typeof text !== 'string') return text
  try {
    // Detecta se string está mal codificada (ex: "SÃO" vindo como "SÃO")
    if (text.includes('Ã') || text.includes('Ç') || text.includes('Â')) {
      return decodeURIComponent(escape(text))
    }
  } catch (e) {
    // Se falhar, retorna o original
  }
  return text
}

// Função recursiva para corrigir encoding em objetos
function fixObjectEncoding(obj) {
  if (typeof obj === 'string') return fixEncoding(obj)
  if (Array.isArray(obj)) return obj.map(fixObjectEncoding)
  if (obj && typeof obj === 'object') {
    const fixed = {}
    for (const key in obj) {
      fixed[key] = fixObjectEncoding(obj[key])
    }
    return fixed
  }
  return obj
}

export async function getOperadoras(page = 1, limit = 20, q = '') {
  const url = new URL(`${BASE_URL}/operadoras`)
  url.searchParams.set('page', page)
  url.searchParams.set('limit', limit)
  if (q) url.searchParams.set('q', q)

  const res = await fetch(url.toString(), fetchOptions)
  if (!res.ok) throw new Error('Erro ao buscar operadoras')
  const data = await res.json()
  return fixObjectEncoding(data)
}

export async function getOperadora(cnpj) {
  const res = await fetch(`${BASE_URL}/operadoras/${encodeURIComponent(cnpj)}`, fetchOptions)
  if (!res.ok) throw new Error('Erro ao buscar operadora')
  const data = await res.json()
  return fixObjectEncoding(data)
}

export async function getOperadoraDetalhes(cnpj) {
  const res = await fetch(`${BASE_URL}/operadoras/${encodeURIComponent(cnpj)}/detalhes`, fetchOptions)
  if (!res.ok) throw new Error('Erro ao buscar detalhes da operadora')
  const data = await res.json()
  return fixObjectEncoding(data)
}

export async function getTop5() {
  const res = await fetch(`${BASE_URL}/estatisticas/top5`, fetchOptions)
  if (!res.ok) throw new Error('Erro ao buscar estatísticas')
  const data = await res.json()
  return fixObjectEncoding(data)
}

export async function getMediaPorConta() {
  const res = await fetch(`${BASE_URL}/estatisticas/media-conta`, fetchOptions)
  if (!res.ok) throw new Error('Erro ao buscar média por conta')
  const data = await res.json()
  return fixObjectEncoding(data)
}

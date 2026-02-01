const BASE_URL = 'http://localhost:8081/api'

export async function getOperadoras(page = 1, limit = 20) {
  const url = new URL(`${BASE_URL}/operadoras`)
  url.searchParams.set('page', page)
  url.searchParams.set('limit', limit)

  const res = await fetch(url.toString())
  if (!res.ok) throw new Error('Erro ao buscar operadoras')
  return res.json()
}

export async function getTop5() {
  const res = await fetch(`${BASE_URL}/estatisticas/top5`)
  if (!res.ok) throw new Error('Erro ao buscar estatísticas')
  return res.json()
}

export async function getMediaPorConta() {
  const res = await fetch(`${BASE_URL}/estatisticas/media-conta`)
  if (!res.ok) throw new Error('Erro ao buscar média por conta')
  return res.json()
}

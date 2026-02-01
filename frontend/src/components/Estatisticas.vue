<template>
  <div>
    <h2>{{ title }}</h2>
    <canvas ref="chartCanvas" width="480" height="320"></canvas>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getTop5 } from '../services/api'
import Chart from 'chart.js/auto'

export default {
  setup() {
    const chartCanvas = ref(null)
    const chartInstance = ref(null)
    const title = ref('Top 5 Operadoras - Maiores Despesas')

    async function renderChart() {
      try {
        const res = await getTop5()
        const data = res.data || []
        const labels = data.map(d => d.nomeFantasia || d.razaoSocial || d.cnpj)
        const values = data.map(d => Number(d.total_despesas || d.total_despesas || 0))

        if (chartInstance.value) chartInstance.value.destroy()

        const ctx = chartCanvas.value.getContext('2d')
        chartInstance.value = new Chart(ctx, {
          type: 'bar',
          data: {
            labels,
            datasets: [{
              label: 'Total despesas',
              data: values,
              backgroundColor: 'rgba(54, 162, 235, 0.6)'
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } }
          }
        })

      } catch (err) {
        console.error('Erro ao carregar estatísticas', err)
      }
    }

    onMounted(renderChart)

    return { chartCanvas, title }
  }
}
</script>

<style scoped>
canvas { width: 100%; height: 320px; }
</style>

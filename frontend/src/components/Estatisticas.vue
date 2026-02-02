<template>
  <div>
    <h2>📊 {{ title }}</h2>
    
    <!-- Loading spinner -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Carregando estatísticas...</p>
    </div>

    <div v-show="!loading" class="chart-container">
      <canvas ref="chartCanvas"></canvas>
    </div>
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
    const loading = ref(false)

    async function renderChart() {
      loading.value = true
      try {
        const res = await getTop5()
        const data = res.data || []
        
        // Extrair labels e valores corretamente
        const labels = data.map(d => d.nomeFantasia || d.razaoSocial || d.cnpj)
        const values = data.map(d => parseFloat(d.totalDespesas || d.total_despesas || 0))

        // Aguardar o próximo tick para garantir que o canvas está no DOM
        loading.value = false
        await new Promise(resolve => setTimeout(resolve, 0))

        if (!chartCanvas.value) {
          console.error('Canvas não encontrado no DOM')
          return
        }

        if (chartInstance.value) chartInstance.value.destroy()

        const ctx = chartCanvas.value.getContext('2d')
        
        // Cores gradiente para as barras
        const colors = [
          'rgba(99, 102, 241, 0.8)',   // Indigo
          'rgba(168, 85, 247, 0.8)',   // Purple
          'rgba(236, 72, 153, 0.8)',   // Pink
          'rgba(251, 146, 60, 0.8)',   // Orange
          'rgba(34, 197, 94, 0.8)'     // Green
        ]
        
        const borderColors = [
          'rgb(99, 102, 241)',
          'rgb(168, 85, 247)',
          'rgb(236, 72, 153)',
          'rgb(251, 146, 60)',
          'rgb(34, 197, 94)'
        ]

        chartInstance.value = new Chart(ctx, {
          type: 'bar',
          data: {
            labels,
            datasets: [{
              label: 'Total de Despesas (R$)',
              data: values,
              backgroundColor: colors,
              borderColor: borderColors,
              borderWidth: 2,
              borderRadius: 8,
              borderSkipped: false
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false, // Para não achatar em telas menores
            plugins: { 
              legend: { 
                display: true,
                position: 'top',
                labels: {
                  font: {
                    size: 13,
                    weight: '600'
                  },
                  color: '#1e293b',
                  padding: 15
                }
              },
              tooltip: {
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                padding: 12,
                titleFont: {
                  size: 14,
                  weight: 'bold'
                },
                bodyFont: {
                  size: 13
                },
                callbacks: {
                  label: function(context) {
                    let label = context.dataset.label || '';
                    if (label) {
                      label += ': ';
                    }
                    label += 'R$ ' + context.parsed.y.toLocaleString('pt-BR', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2
                    });
                    return label;
                  }
                }
              }
            },
            scales: { 
              y: { 
                beginAtZero: true,
                ticks: { 
                  callback: v => 'R$ ' + v.toLocaleString('pt-BR'),
                  font: {
                    size: 11
                  },
                  color: '#64748b'
                },
                grid: {
                  color: 'rgba(0, 0, 0, 0.05)'
                }
              },
              x: {
                ticks: {
                  font: {
                    size: 11
                  },
                  color: '#64748b',
                  maxRotation: 45, // Legendas inclinadas a 45 graus
                  minRotation: 45,
                  autoSkip: false
                },
                grid: {
                  display: false
                }
              }
            },
            animation: {
              duration: 1000,
              easing: 'easeOutQuart'
            }
          }
        })

      } catch (err) {
        console.error('Erro ao carregar estatísticas', err)
        loading.value = false
      }
    }

    onMounted(renderChart)

    return { chartCanvas, title, loading }
  }
}
</script>

<style scoped>
/* Loading Spinner */
.loading-container {
  text-align: center;
  padding: 80px 20px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #2563eb;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  color: #64748b;
  font-size: 14px;
}

/* Chart Container */
.chart-container {
  position: relative;
  width: 100%;
  height: 400px; /* Altura fixa para não achatar */
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #e0e7ff 100%);
  border-radius: 8px;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .chart-container {
    height: 350px;
  }
}
</style>

<template>
  <div>
    <h2>📋 Operadoras de Saúde</h2>

    <!-- Loading spinner -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Carregando dados...</p>
    </div>

    <div v-else>
      <!-- Campo de busca com ícone -->
      <div class="search-container">
        <span class="search-icon">🔍</span>
        <input 
          class="search-input"
          placeholder="Buscar por CNPJ ou Razão Social (pressione Enter)" 
          @keyup.enter="doSearch" 
          v-model="q"
        />
      </div>

      <!-- Tabela com estilização -->
      <div class="table-wrapper">
        <table class="styled-table">
          <thead>
            <tr>
              <th>Razão Social</th>
              <th>CNPJ</th>
              <th>UF</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in operadoras" :key="o.cnpj">
              <td>{{ o.razaoSocial }}</td>
              <td>{{ formatCNPJ(o.cnpj) }}</td>
              <td><span class="uf-badge">{{ o.uf || '-' }}</span></td>
              <td>
                <button class="btn-details" @click.prevent="openDetails(o)">
                  Ver Detalhes
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Paginação -->
      <div class="pager">
        <button class="btn-pager" @click="prev" :disabled="page<=1">
          ← Anterior
        </button>
        <span class="page-info">Página {{ page }} / {{ totalPages }}</span>
        <button class="btn-pager" @click="next" :disabled="page>=totalPages">
          Próxima →
        </button>
      </div>
    </div>

    <!-- Modal de detalhes -->
    <div v-if="showModal" class="modal" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <div>
            <h3>{{ selectedOperadora.razaoSocial }}</h3>
            <p class="subtitle" v-if="selectedOperadora.nomeFantasia">{{ selectedOperadora.nomeFantasia }}</p>
          </div>
          <button class="btn-close" @click="closeModal">✕</button>
        </div>
        
        <div class="modal-body">
          <!-- Seção de Informações Básicas -->
          <div class="info-section">
            <h4>📋 Informações Cadastrais</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">CNPJ:</span>
                <span class="info-value">{{ formatCNPJ(selectedOperadora.cnpj) }}</span>
              </div>
              <div class="info-item" v-if="selectedOperadora.modalidade">
                <span class="info-label">Modalidade:</span>
                <span class="info-value">{{ selectedOperadora.modalidade }}</span>
              </div>
            </div>
          </div>

          <!-- Seção de Contato e Localização -->
          <div class="info-section" v-if="selectedOperadora.email || selectedOperadora.telefone || selectedOperadora.cidade">
            <h4>📍 Informações de Contato e Localização</h4>
            <div class="info-grid">
              <div class="info-item" v-if="selectedOperadora.email">
                <span class="info-label">✉️ Email:</span>
                <span class="info-value">{{ selectedOperadora.email }}</span>
              </div>
              <div class="info-item" v-if="selectedOperadora.telefone">
                <span class="info-label">📞 Telefone:</span>
                <span class="info-value">{{ selectedOperadora.telefone }}</span>
              </div>
              <div class="info-item" v-if="selectedOperadora.cidade">
                <span class="info-label">🏢 Localização:</span>
                <span class="info-value">{{ selectedOperadora.cidade }} - {{ selectedOperadora.uf }}</span>
              </div>
            </div>
          </div>
          
          <!-- Histórico de Despesas em Tabela -->
          <div class="historico-section">
            <h4>💰 Histórico de Despesas</h4>
            
            <!-- Empty State -->
            <div v-if="!selectedOperadora.historicoDespesas || selectedOperadora.historicoDespesas.length === 0" class="empty-state">
              <div class="empty-icon">📊</div>
              <p class="empty-message">Nenhum histórico financeiro encontrado</p>
              <p class="empty-submessage">Esta operadora não possui despesas registradas no sistema.</p>
            </div>

            <!-- Tabela com dados -->
            <div v-else class="table-historico-wrapper">
              <table class="table-historico">
                <thead>
                  <tr>
                    <th>Ano</th>
                    <th>Trimestre</th>
                    <th class="text-right">Valor (R$)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(desp, idx) in selectedOperadora.historicoDespesas" :key="idx">
                    <td>{{ desp.ano }}</td>
                    <td><span class="badge-trimestre">{{ desp.trimestre }}</span></td>
                    <td class="text-right valor-destaque">{{ formatCurrency(desp.valor) }}</td>
                  </tr>
                </tbody>
                <tfoot v-if="calcularTotal(selectedOperadora.historicoDespesas) > 0">
                  <tr>
                    <td colspan="2" class="text-right"><strong>Total:</strong></td>
                    <td class="text-right valor-total">
                      <strong>{{ formatCurrency(calcularTotal(selectedOperadora.historicoDespesas)) }}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getOperadoras, getOperadoraDetalhes } from '../services/api'

export default {
  setup() {
    const operadoras = ref([])
    const page = ref(1)
    const limit = ref(20)
    const totalPages = ref(1)
    const q = ref('')
    const showModal = ref(false)
    const selectedOperadora = ref({})
    const loading = ref(false)

    async function load() {
      loading.value = true
      try {
        const res = await getOperadoras(page.value, limit.value, q.value)
        operadoras.value = res.data || []
        totalPages.value = res.totalPages || 1
      } catch (err) {
        console.error(err)
        operadoras.value = []
      } finally {
        loading.value = false
      }
    }

    function next() { if (page.value < totalPages.value) { page.value++; load() } }
    function prev() { if (page.value > 1) { page.value--; load() } }

    function doSearch() { page.value = 1; load() }

    function formatCNPJ(v = '') {
      const nums = (v || '').replace(/\D/g, '')
      if (nums.length !== 14) return v
      return nums.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5')
    }

    function formatNumber(n) { return Number(n).toLocaleString('pt-BR', {minimumFractionDigits:2, maximumFractionDigits:2}) }

    function formatCurrency(value) {
      const numero = Number(value) || 0
      return new Intl.NumberFormat('pt-BR', { 
        style: 'currency', 
        currency: 'BRL',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(numero)
    }

    async function openDetails(o) {
      try {
        const detalhes = await getOperadoraDetalhes(o.cnpj)
        selectedOperadora.value = detalhes || o
        showModal.value = true
      } catch (err) {
        console.error('Erro ao carregar detalhes', err)
        selectedOperadora.value = o
        showModal.value = true
      }
    }

    function closeModal() { showModal.value = false }

    function calcularTotal(despesas) {
      if (!despesas || !Array.isArray(despesas)) return 0
      return despesas.reduce((sum, desp) => sum + Number(desp.valor || 0), 0)
    }

    onMounted(load)

    return { 
      operadoras, 
      page, 
      totalPages, 
      next, 
      prev, 
      q, 
      doSearch, 
      formatCNPJ, 
      showModal, 
      selectedOperadora, 
      openDetails, 
      closeModal, 
      formatNumber,
      formatCurrency,
      loading,
      calcularTotal
    }
  }
}
</script>

<style scoped>
/* Loading Spinner */
.loading-container {
  text-align: center;
  padding: 60px 20px;
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

/* Search Container */
.search-container {
  position: relative;
  margin-bottom: 20px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: #f8fafc;
}

.search-input:focus {
  outline: none;
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Table Wrapper */
.table-wrapper {
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.styled-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

.styled-table thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  position: sticky;
  top: 0;
  z-index: 10;
}

.styled-table th {
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.styled-table tbody tr {
  border-bottom: 1px solid #e2e8f0;
  transition: background-color 0.2s ease;
}

/* Zebra Striping */
.styled-table tbody tr:nth-child(even) {
  background-color: #f8fafc;
}

/* Hover Effect */
.styled-table tbody tr:hover {
  background-color: #e0e7ff;
  cursor: pointer;
}

.styled-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: #1e293b;
}

.uf-badge {
  display: inline-block;
  padding: 4px 10px;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 12px;
  font-weight: 600;
  font-size: 12px;
}

/* Botão Ver Detalhes */
.btn-details {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
}

.btn-details:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
}

.btn-details:active {
  transform: translateY(0);
}

/* Paginação */
.pager {
  margin-top: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
}

.btn-pager {
  padding: 10px 20px;
  border: 2px solid #2563eb;
  background: white;
  color: #2563eb;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-pager:hover:not([disabled]) {
  background: #2563eb;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.2);
}

.btn-pager[disabled] {
  opacity: 0.4;
  cursor: not-allowed;
  border-color: #cbd5e1;
  color: #cbd5e1;
}

.page-info {
  font-weight: 600;
  color: #475569;
  font-size: 14px;
}

/* Modal */
.modal {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 640px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  border-bottom: 2px solid #e2e8f0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.modal-header .subtitle {
  margin: 4px 0 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.btn-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.modal-body {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

/* Seções de informação */
.info-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.info-section:last-child {
  border-bottom: none;
}

.info-section h4 {
  font-size: 16px;
  margin: 0 0 16px 0;
  color: #475569;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

/* Histórico - Tabela */
.historico-section {
  margin-top: 24px;
}

.historico-section h4 {
  font-size: 16px;
  margin-bottom: 16px;
  color: #1e293b;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #e0e7ff 100%);
  border-radius: 12px;
  border: 2px dashed #cbd5e1;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-message {
  font-size: 16px;
  font-weight: 600;
  color: #475569;
  margin: 0 0 8px 0;
}

.empty-submessage {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.table-historico-wrapper {
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.table-historico {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

.table-historico thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.table-historico th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table-historico tbody tr {
  border-bottom: 1px solid #e2e8f0;
  transition: background-color 0.2s ease;
}

.table-historico tbody tr:nth-child(even) {
  background-color: #f8fafc;
}

.table-historico tbody tr:hover {
  background-color: #e0e7ff;
}

.table-historico td {
  padding: 12px 16px;
  font-size: 14px;
  color: #1e293b;
}

.table-historico .text-right {
  text-align: right;
}

.badge-trimestre {
  display: inline-block;
  padding: 4px 12px;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 12px;
  font-weight: 600;
  font-size: 12px;
}

.valor-destaque {
  color: #059669;
  font-weight: 600;
}

.table-historico tfoot {
  background: #f1f5f9;
  border-top: 2px solid #cbd5e1;
}

.table-historico tfoot td {
  padding: 14px 16px;
  font-size: 15px;
}

.valor-total {
  color: #059669;
  font-size: 16px;
}

/* Responsividade */
@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .modal-content {
    width: 95%;
    max-height: 95vh;
  }
}
</style>

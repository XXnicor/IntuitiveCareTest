<template>
  <div>
    <h2>Operadoras</h2>
    <table>
      <thead>
        <tr>
          <th>CNPJ</th>
          <th>Nome Fantasia</th>
          <th>Razão Social</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in operadoras" :key="o.cnpj">
          <td>{{ o.cnpj }}</td>
          <td>{{ o.nomeFantasia }}</td>
          <td>{{ o.razaoSocial }}</td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <button class="btn" @click="prev" :disabled="page<=1">Anterior</button>
      <span> Página {{ page }} / {{ totalPages }} </span>
      <button class="btn" @click="next" :disabled="page>=totalPages">Próxima</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { getOperadoras } from '../services/api'

export default {
  setup() {
    const operadoras = ref([])
    const page = ref(1)
    const limit = ref(20)
    const totalPages = ref(1)

    async function load() {
      try {
        const res = await getOperadoras(page.value, limit.value)
        operadoras.value = res.data || []
        totalPages.value = res.totalPages || 1
      } catch (err) {
        console.error(err)
        operadoras.value = []
      }
    }

    function next() { if (page.value < totalPages.value) { page.value++; load() } }
    function prev() { if (page.value > 1) { page.value--; load() } }

    onMounted(load)

    return { operadoras, page, totalPages, next, prev }
  }
}
</script>

<style scoped>
.btn[disabled] { opacity: 0.5; cursor: not-allowed }
</style>

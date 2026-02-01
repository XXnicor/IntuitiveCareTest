<template>
  <div class="container">
    <header>
      <h1>Intuitive Care — Dashboard</h1>
      <p>Frontend mínimo (Vue + Vite) para consumir a API Spring Boot</p>
    </header>

    <main>
      <section class="left">
        <OperadorasTable />
      </section>
      <section class="right">
        <Estatisticas />
      </section>
    </main>
  </div>
</template>

<script>
import OperadorasTable from './components/OperadorasTable.vue'
import Estatisticas from './components/Estatisticas.vue'

export default {
  components: { OperadorasTable, Estatisticas }
}
</script>

<style>
body { font-family: Inter, Arial, sans-serif; margin: 0; }
.container { padding: 16px; }
header { margin-bottom: 16px; }
main { display: flex; gap: 16px; }
.left { flex: 1; }
.right { flex: 0 0 520px; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px; border-bottom: 1px solid #ddd; text-align: left; }
.pager { margin-top: 8px; display:flex; gap:8px; align-items:center }
.btn { padding:6px 10px; border:1px solid #ccc; background:#fff; cursor:pointer }
</style>

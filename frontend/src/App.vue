<template>
  <div class="container">
    <header>
      <h1>🏥 Intuitive Care — Dashboard ANS</h1>
      <p>Sistema de monitoramento de operadoras de saúde</p>
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
:root {
  --primary-color: #2563eb;
  --primary-hover: #1d4ed8;
  --secondary-color: #64748b;
  --bg-color: #f8fafc;
  --card-bg: #ffffff;
  --text-color: #1e293b;
  --border-color: #e2e8f0;
  --shadow: 0 1px 3px rgba(0,0,0,0.1);
  --shadow-lg: 0 10px 25px rgba(0,0,0,0.15);
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body { 
  font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Inter, Arial, sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  color: var(--text-color);
  line-height: 1.6;
}

.container { 
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

header { 
  background: var(--card-bg);
  padding: 24px 32px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-lg);
  border-left: 4px solid var(--primary-color);
}

header h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 8px;
}

header p {
  color: var(--secondary-color);
  font-size: 14px;
}

main { 
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.left { 
  flex: 1;
  min-width: 600px;
  background: var(--card-bg);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
}

.right { 
  flex: 0 0 520px;
  background: var(--card-bg);
  padding: 24px;
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
}

h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--border-color);
}

/* Responsividade */
@media (max-width: 1200px) {
  .right {
    flex: 1;
    min-width: 100%;
  }
}

@media (max-width: 768px) {
  .left {
    min-width: 100%;
  }
  
  header h1 {
    font-size: 22px;
  }
  
  main {
    flex-direction: column;
  }
}
</style>

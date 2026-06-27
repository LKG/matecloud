<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

interface DashboardVO {
  userCount: number
  todayLoginCount: number
  todayOpCount: number
  onlineCount: number
}

const router = useRouter()
const username = ref('')
const data = ref<DashboardVO | null>(null)
const loading = ref(false)

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || 'http://127.0.0.1:9010/api/v1'

async function load() {
  loading.value = true
  try {
    const response = await fetch(`${baseUrl}/admin/monitor/dashboard`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('mate.token') || ''}` },
    })
    const body = await response.json() as { code?: string; data?: DashboardVO }
    if (body.code === '200' && body.data) {
      data.value = body.data
    }
  } catch (e) {
    // network errors are surfaced via the dashboard tile placeholders
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  username.value = localStorage.getItem('mate.username') || 'Admin'
  load()
})

function logout() {
  localStorage.removeItem('mate.token')
  localStorage.removeItem('mate.username')
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">MateCloud</div>
      <nav>
        <router-link to="/home" class="nav-item">Dashboard</router-link>
        <router-link to="/users" class="nav-item">Users</router-link>
      </nav>
    </aside>

    <main>
      <header>
        <span class="spacer"></span>
        <span class="user">{{ username }}</span>
        <el-button link @click="logout">Sign out</el-button>
      </header>

      <section class="stats">
        <el-card class="tile" shadow="hover">
          <div class="tile-title">Users</div>
          <div class="tile-num">{{ data?.userCount ?? '—' }}</div>
        </el-card>
        <el-card class="tile" shadow="hover">
          <div class="tile-title">Online</div>
          <div class="tile-num">{{ data?.onlineCount ?? '—' }}</div>
        </el-card>
        <el-card class="tile" shadow="hover">
          <div class="tile-title">Today logins</div>
          <div class="tile-num">{{ data?.todayLoginCount ?? '—' }}</div>
        </el-card>
        <el-card class="tile" shadow="hover">
          <div class="tile-title">Today ops</div>
          <div class="tile-num">{{ data?.todayOpCount ?? '—' }}</div>
        </el-card>
      </section>

      <el-card style="margin: 0 24px 24px;">
        <h2 style="margin-top:0">Welcome, {{ username }}</h2>
        <p>Connected to <code>{{ baseUrl }}</code>. The full admin console is
        available at the web URL configured in your deployment.</p>
        <el-button type="primary" :loading="loading" @click="load">Refresh</el-button>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.layout {
  height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
}
.sidebar {
  background: #1f2937;
  color: #fff;
  padding: 24px 0;
}
.brand {
  padding: 0 24px;
  font-size: 18px;
  font-weight: 600;
  color: #1976d2;
  margin-bottom: 24px;
}
nav { display: flex; flex-direction: column; }
.nav-item {
  padding: 12px 24px;
  color: #d1d5db;
  text-decoration: none;
  font-size: 14px;
}
.nav-item:hover { background: #374151; }
.nav-item.router-link-active { background: #2563eb; color: #fff; }

main {
  background: #f5f7fa;
  overflow-y: auto;
  padding-bottom: 24px;
}
header {
  background: #fff;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #e5e7eb;
}
.spacer { flex: 1; }
.user { font-weight: 500; }

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 24px;
}
.tile { text-align: center; }
.tile-title { color: #6b7280; font-size: 13px; }
.tile-num {
  font-size: 32px;
  font-weight: 600;
  color: #1976d2;
  margin-top: 8px;
}
</style>

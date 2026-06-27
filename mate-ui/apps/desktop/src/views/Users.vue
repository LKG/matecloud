<script setup lang="ts">
import { MateMessage } from '@matecloud/ui'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

interface UserRow {
  userId: string
  username: string
  realName?: string
  mobile?: string
  email?: string
  status?: string | number
  createdAt?: string
}

const router = useRouter()
const rows = ref<UserRow[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const loading = ref(false)
const username = ref('')

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || 'http://127.0.0.1:9010/api/v1'

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({
      pageNum: String(pageNum.value),
      pageSize: String(pageSize.value),
    })
    if (keyword.value) params.set('keyword', keyword.value)
    const response = await fetch(`${baseUrl}/users?${params}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('mate.token') || ''}` },
    })
    const body = await response.json() as {
      code?: string
      msg?: string
      data?: { list?: UserRow[]; total?: number }
    }
    if (body.code === '200' && body.data) {
      rows.value = body.data.list ?? []
      total.value = body.data.total ?? 0
    } else {
      MateMessage.error(body.msg || 'Failed to load users')
    }
  } catch (e: any) {
    MateMessage.error(e?.message || 'Network error')
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

function pageChange(p: number) {
  pageNum.value = p
  load()
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

      <el-card style="margin: 24px;">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="Search by username / mobile / real name"
            clearable
            @keyup.enter="search"
            style="width: 320px;"
          />
          <el-button type="primary" :loading="loading" @click="search">Search</el-button>
        </div>

        <el-table :data="rows" :loading="loading" stripe style="margin-top: 16px;">
          <el-table-column prop="username" label="Username" width="180" />
          <el-table-column prop="realName" label="Real name" width="160" />
          <el-table-column prop="mobile" label="Mobile" width="160" />
          <el-table-column prop="email" label="Email" />
          <el-table-column prop="status" label="Status" width="100" />
          <el-table-column prop="createdAt" label="Created" width="200" />
        </el-table>

        <el-pagination
          style="margin-top: 16px; text-align: right;"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="pageChange"
        />
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

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>

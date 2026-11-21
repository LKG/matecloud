<template>
  <MatePageCard title="客户 360" description="客户全景视图: 基本信息 / 联系人 / 跟进 / 商机 / 合同 / AI 洞察">
    <template #actions>
      <MateButton @click="goBack"><template #icon><ArrowLeft :size="14" /></template>返回</MateButton>
    </template>

    <div v-loading="loading">
      <template v-if="customer">
        <div class="cust-head">
          <MateEntityCell :name="customer.customerName" :sub="customer.industry || '—'" avatar />
          <div class="cust-meta">
            <MateTag v-if="customer.level" :variant="customer.level === 'A' ? 'danger' : 'info'">{{ customer.level }} 类</MateTag>
            <MateTag :variant="customer.poolStatus === 1 ? 'warning' : 'success'" dot>{{ customer.poolStatus === 1 ? '公海' : '私海' }}</MateTag>
            <span v-if="customer.ownerName" class="cust-owner">负责人: {{ customer.ownerName }}</span>
          </div>
        </div>

        <el-tabs v-model="tab">
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="客户名称">{{ customer.customerName }}</el-descriptions-item>
              <el-descriptions-item label="行业">{{ customer.industry || '—' }}</el-descriptions-item>
              <el-descriptions-item label="电话">{{ customer.phone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="官网">{{ customer.website || '—' }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ customer.address || '—' }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ customer.source || '—' }}</el-descriptions-item>
              <el-descriptions-item label="最近跟进">{{ customer.recentFollowTime || '—' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ customer.createdAt || '—' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ customer.remark || '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane :label="`联系人 (${contacts.length})`" name="contacts">
            <el-table :data="contacts" size="small" empty-text="暂无联系人">
              <el-table-column prop="contactName" label="姓名" />
              <el-table-column prop="position" label="职位" />
              <el-table-column prop="mobile" label="手机" />
              <el-table-column prop="email" label="邮箱" />
              <el-table-column label="主联系人" width="90">
                <template #default="{ row }"><MateTag v-if="row.isPrimary === 1" variant="success">主</MateTag></template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`跟进记录 (${followUps.length})`" name="followups">
            <el-empty v-if="!followUps.length" description="暂无跟进记录" />
            <el-timeline v-else>
              <el-timeline-item v-for="f in followUps" :key="f.id" :timestamp="f.createdAt" placement="top">
                <div class="fu-content">{{ f.content }}</div>
                <div v-if="f.nextContent" class="fu-next">下次: {{ f.nextContent }} {{ f.nextTime || '' }}</div>
                <div class="fu-user">{{ f.userName }}</div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>

          <el-tab-pane :label="`商机 (${businesses.length})`" name="business">
            <el-table :data="businesses" size="small" empty-text="暂无商机">
              <el-table-column prop="businessName" label="商机名称" min-width="160" />
              <el-table-column prop="stage" label="阶段" width="120" />
              <el-table-column prop="money" label="金额" width="120" />
              <el-table-column prop="ownerName" label="负责人" width="110" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`合同 (${contracts.length})`" name="contract">
            <el-table :data="contracts" size="small" empty-text="暂无合同">
              <el-table-column prop="contractName" label="合同名称" min-width="160" />
              <el-table-column prop="contractNum" label="合同编号" width="150" />
              <el-table-column prop="money" label="金额" width="120" />
              <el-table-column prop="status" label="状态" width="110" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="AI 洞察" name="ai">
            <div class="ai-actions">
              <MateButton variant="primary" :loading="insightLoading" @click="loadInsight">生成客户洞察</MateButton>
            </div>
            <div v-if="insight" class="insight">
              <p class="insight-summary">{{ insight.summary }}</p>
              <el-descriptions :column="3" border size="small" class="insight-stats">
                <el-descriptions-item label="商机总数">{{ insight.businessTotal }}</el-descriptions-item>
                <el-descriptions-item label="进行中">{{ insight.businessActive }}</el-descriptions-item>
                <el-descriptions-item label="合同数">{{ insight.contractCount }}</el-descriptions-item>
                <el-descriptions-item label="合同金额">{{ insight.totalMoney }}</el-descriptions-item>
                <el-descriptions-item label="已回款">{{ insight.receivedMoney }}</el-descriptions-item>
                <el-descriptions-item label="待回款">{{ insight.receivableMoney }}</el-descriptions-item>
              </el-descriptions>
              <div v-if="insight.risks?.length" class="insight-block">
                <h4>风险提示</h4>
                <el-alert v-for="(r, i) in insight.risks" :key="'r' + i" :title="r" type="warning" :closable="false" class="insight-line" />
              </div>
              <div v-if="insight.suggestions?.length" class="insight-block">
                <h4>行动建议</h4>
                <ul class="insight-list"><li v-for="(s, i) in insight.suggestions" :key="'s' + i">{{ s }}</li></ul>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else description="客户不存在或无权访问" />
    </div>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import {
  customerApi, businessApi, contractApi, crmAiApi,
  type Customer, type Contact, type FollowUp, type Business, type Contract, type CustomerInsight,
} from '@matecloud/core'
import { MatePageCard, MateButton, MateTag, MateEntityCell, MateMessage } from '@matecloud/ui'

defineOptions({ name: 'CrmCustomerDetail' })

const route = useRoute()
const router = useRouter()
const customerId = String(route.query.id || '')

const loading = ref(false)
const tab = ref('basic')
const customer = ref<Customer | null>(null)
const contacts = ref<Contact[]>([])
const followUps = ref<FollowUp[]>([])
const businesses = ref<Business[]>([])
const contracts = ref<Contract[]>([])

const insight = ref<CustomerInsight | null>(null)
const insightLoading = ref(false)

function goBack() { router.back() }

async function loadAll() {
  if (!customerId) { MateMessage.error('缺少客户 ID'); return }
  loading.value = true
  try {
    const d = await customerApi.detail(customerId)
    customer.value = d.data.customer
    contacts.value = d.data.contacts || []
    followUps.value = d.data.followUps || []
    const [b, c] = await Promise.all([
      businessApi.page({ customerId, pageSize: 100 }),
      contractApi.page({ customerId, pageSize: 100 }),
    ])
    businesses.value = b.data.list
    contracts.value = c.data.list
  } catch (e: any) {
    MateMessage.error(e?.msg || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadInsight() {
  insightLoading.value = true
  try {
    const res = await crmAiApi.insight(customerId)
    insight.value = res.data
  } catch (e: any) {
    MateMessage.error(e?.msg || '洞察生成失败')
  } finally {
    insightLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.cust-head { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.cust-meta { display: flex; align-items: center; gap: 10px; }
.cust-owner { color: var(--el-text-color-secondary); font-size: 13px; }
.fu-content { font-size: 14px; }
.fu-next { color: var(--el-color-primary); font-size: 13px; margin-top: 4px; }
.fu-user { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px; }
.ai-actions { margin-bottom: 12px; }
.insight-summary { font-size: 14px; line-height: 1.7; color: var(--el-text-color-primary); }
.insight-stats { margin: 12px 0; }
.insight-block h4 { margin: 12px 0 6px; }
.insight-line { margin-bottom: 6px; }
.insight-list { margin: 0; padding-left: 20px; line-height: 1.8; }
</style>

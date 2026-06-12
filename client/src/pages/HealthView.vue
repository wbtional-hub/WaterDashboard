<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getHealth, type HealthData } from '@/api/health'
import type { RequestError } from '@/api/request'

const loading = ref(false)
const health = ref<HealthData | null>(null)
const traceId = ref('')
const errorMessage = ref('')

async function loadHealth() {
  loading.value = true
  errorMessage.value = ''
  health.value = null

  try {
    const response = await getHealth()
    health.value = response.data
    traceId.value = response.traceId
  } catch (error) {
    const requestError = error as RequestError
    errorMessage.value = requestError.message
    traceId.value = requestError.traceId ?? ''
  } finally {
    loading.value = false
  }
}

onMounted(loadHealth)
</script>

<template>
  <section class="health-section">
    <div class="page-heading">
      <div>
        <p class="section-label">服务诊断</p>
        <h2>后端健康检查</h2>
        <p>调用 <code>GET /api/health</code>，确认服务、PostgreSQL、PostGIS 与 traceId 链路。</p>
      </div>
      <button type="button" :disabled="loading" @click="loadHealth">
        {{ loading ? '检查中...' : '重新检查' }}
      </button>
    </div>

    <div v-if="health" class="health-result" :class="{ 'success-result': health.status === 'UP' }">
      <div class="result-title">
        <span class="status-dot" :class="{ 'status-dot-warning': health.status !== 'UP' }"></span>
        <strong>服务状态：{{ health.status }}</strong>
      </div>
      <dl class="detail-list">
        <div><dt>应用名称</dt><dd>{{ health.applicationName }}</dd></div>
        <div><dt>版本号</dt><dd>{{ health.version }}</dd></div>
        <div><dt>数据库状态</dt><dd>{{ health.databaseStatus }}</dd></div>
        <div><dt>PostgreSQL</dt><dd>{{ health.postgresConnected ? '已连接' : '未连接' }}</dd></div>
        <div><dt>PostGIS</dt><dd>{{ health.postgisAvailable ? '可用' : '不可用' }}</dd></div>
        <div><dt>当前时间</dt><dd>{{ health.currentTime }}</dd></div>
        <div><dt>traceId</dt><dd><code>{{ traceId }}</code></dd></div>
      </dl>
    </div>

    <div v-else-if="errorMessage" class="health-result error-result">
      <strong>暂时无法连接后端服务</strong>
      <p>{{ errorMessage }}</p>
      <p v-if="traceId">traceId：<code>{{ traceId }}</code></p>
      <p v-else>请确认后端服务已启动，并稍后重试。</p>
    </div>

    <div v-else class="health-result">
      <p>正在获取服务状态...</p>
    </div>
  </section>
</template>


<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createDataSource,
  disableDataSource,
  listDataSources,
  testDataSource,
  testTempDataSource,
  updateDataSource,
  type DataSourceItem,
  type DataSourcePayload,
  type DataSourceTestResult,
} from '@/api/dataSources'
import type { RequestError } from '@/api/request'

const filters = reactive({
  name: '',
  type: 'POSTGRESQL',
  status: '',
  enabled: '' as boolean | '',
})

const emptyForm = (): DataSourcePayload => ({
  name: '',
  type: 'POSTGRESQL',
  host: '127.0.0.1',
  port: 5432,
  database: '',
  username: '',
  password: '',
  schema: 'public',
  remark: '',
  enabled: true,
})

const items = ref<DataSourceItem[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref('')
const errorMessage = ref('')
const activeId = ref('')
const form = reactive<DataSourcePayload>(emptyForm())
const testResult = ref<DataSourceTestResult | null>(null)

const isEditing = computed(() => Boolean(activeId.value))

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listDataSources({
      name: filters.name || undefined,
      type: filters.type || undefined,
      status: filters.status || undefined,
      enabled: filters.enabled,
    })
    items.value = response.data.items
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function startCreate() {
  Object.assign(form, emptyForm())
  activeId.value = ''
  testResult.value = null
  errorMessage.value = ''
}

function startEdit(item: DataSourceItem) {
  activeId.value = item.id
  Object.assign(form, {
    name: item.name,
    type: item.type,
    host: item.host,
    port: item.port,
    database: item.database,
    username: item.username,
    password: '',
    schema: item.schema || 'public',
    remark: item.remark || '',
    enabled: item.enabled,
  })
  testResult.value = null
  errorMessage.value = ''
}

async function saveForm() {
  saving.value = true
  errorMessage.value = ''
  testResult.value = null
  try {
    if (isEditing.value) {
      await updateDataSource(activeId.value, form)
    } else {
      await createDataSource(form)
    }
    startCreate()
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function testSaved(item: DataSourceItem) {
  testingId.value = item.id
  errorMessage.value = ''
  testResult.value = null
  try {
    const response = await testDataSource(item.id)
    testResult.value = response.data
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    testingId.value = ''
  }
}

async function testCurrentForm() {
  testingId.value = 'temp'
  errorMessage.value = ''
  testResult.value = null
  try {
    const response = await testTempDataSource(form)
    testResult.value = response.data
  } catch (error) {
    showError(error)
  } finally {
    testingId.value = ''
  }
}

async function disableItem(item: DataSourceItem) {
  if (!window.confirm(`确认停用数据源“${item.name}”？`)) {
    return
  }
  testingId.value = item.id
  errorMessage.value = ''
  try {
    await disableDataSource(item.id)
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    testingId.value = ''
  }
}

function showError(error: unknown) {
  const requestError = error as RequestError
  errorMessage.value = requestError.traceId
    ? `${requestError.message}（traceId: ${requestError.traceId}）`
    : requestError.message || '操作失败，请稍后重试'
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(loadList)
</script>

<template>
  <section class="page-card">
    <div class="page-heading">
      <div>
        <p class="eyebrow">DATA SOURCE CENTER</p>
        <h2>数据源配置中心</h2>
        <p>第一阶段只支持 PostgreSQL 数据源配置和连接测试，不执行 SQL 预览。</p>
      </div>
      <button type="button" class="secondary-button" @click="startCreate">新增数据源</button>
    </div>

    <div class="filters-panel">
      <label>
        名称
        <input v-model="filters.name" placeholder="按名称筛选" />
      </label>
      <label>
        类型
        <select v-model="filters.type">
          <option value="">全部</option>
          <option value="POSTGRESQL">PostgreSQL</option>
        </select>
      </label>
      <label>
        状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
      <label>
        启用状态
        <select v-model="filters.enabled">
          <option value="">全部</option>
          <option :value="true">启用</option>
          <option :value="false">停用</option>
        </select>
      </label>
      <button type="button" class="primary-button" :disabled="loading" @click="loadList">
        {{ loading ? '查询中...' : '查询' }}
      </button>
    </div>

    <p v-if="errorMessage" class="message error-message">{{ errorMessage }}</p>

    <div class="data-source-grid">
      <div class="table-panel">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>类型</th>
              <th>主机</th>
              <th>端口</th>
              <th>数据库</th>
              <th>状态</th>
              <th>最近测试</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && items.length === 0">
              <td colspan="9" class="empty-cell">暂无数据源</td>
            </tr>
            <tr v-for="item in items" :key="item.id">
              <td>
                <strong>{{ item.name }}</strong>
                <span class="muted-line">{{ item.username }} / {{ item.schema }}</span>
              </td>
              <td>{{ item.type }}</td>
              <td>{{ item.host }}</td>
              <td>{{ item.port }}</td>
              <td>{{ item.database }}</td>
              <td>
                <span :class="['status-pill', item.enabled ? 'status-up' : 'status-down']">
                  {{ item.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(item.lastTestTime) }}</td>
              <td>{{ formatTime(item.updatedAt) }}</td>
              <td class="actions-cell">
                <button type="button" @click="startEdit(item)">编辑</button>
                <button type="button" :disabled="testingId === item.id" @click="testSaved(item)">
                  {{ testingId === item.id ? '测试中' : '测试' }}
                </button>
                <button type="button" class="danger-button" :disabled="!item.enabled" @click="disableItem(item)">
                  停用
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <form class="form-panel" @submit.prevent="saveForm">
        <h3>{{ isEditing ? '编辑数据源' : '新增数据源' }}</h3>
        <div class="form-grid">
          <label>
            数据源名称
            <input v-model.trim="form.name" required placeholder="例如：生产只读库" />
          </label>
          <label>
            类型
            <select v-model="form.type">
              <option value="POSTGRESQL">PostgreSQL</option>
            </select>
          </label>
          <label>
            Host
            <input v-model.trim="form.host" required placeholder="127.0.0.1" />
          </label>
          <label>
            Port
            <input v-model.number="form.port" required type="number" min="1" max="65535" />
          </label>
          <label>
            Database
            <input v-model.trim="form.database" required placeholder="water_dashboard" />
          </label>
          <label>
            Schema
            <input v-model.trim="form.schema" placeholder="public" />
          </label>
          <label>
            Username
            <input v-model.trim="form.username" required placeholder="只读账号" autocomplete="off" />
          </label>
          <label>
            Password
            <input
              v-model="form.password"
              :required="!isEditing"
              type="password"
              autocomplete="new-password"
              :placeholder="isEditing ? '留空则保持原密码' : '请输入密码'"
            />
          </label>
          <label class="full-field">
            备注
            <textarea v-model.trim="form.remark" rows="3" placeholder="用途、环境或管理员备注"></textarea>
          </label>
          <label class="checkbox-field">
            <input v-model="form.enabled" type="checkbox" />
            保存后启用
          </label>
        </div>

        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <button type="button" class="secondary-button" :disabled="testingId === 'temp'" @click="testCurrentForm">
            {{ testingId === 'temp' ? '测试中...' : '测试当前参数' }}
          </button>
        </div>

        <div v-if="testResult" :class="['test-result', testResult.success ? 'test-ok' : 'test-fail']">
          <strong>{{ testResult.message }}</strong>
          <span v-if="testResult.success">延迟：{{ testResult.latencyMs }}ms</span>
          <span v-if="testResult.databaseVersion">版本：{{ testResult.databaseVersion }}</span>
          <span v-if="testResult.errorSummary">错误摘要：{{ testResult.errorSummary }}</span>
          <span>traceId：{{ testResult.traceId }}</span>
        </div>
      </form>
    </div>
  </section>
</template>

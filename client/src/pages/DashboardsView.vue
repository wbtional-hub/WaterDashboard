<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createDashboard,
  disableDashboard,
  enableDashboard,
  getDashboardDraft,
  listDashboards,
  saveDashboardDraft,
  updateDashboard,
  type DashboardDraft,
  type DashboardItem,
  type DashboardPayload,
} from '@/api/dashboards'
import type { RequestError } from '@/api/request'

const filters = reactive({
  name: '',
  dashboardCode: '',
  status: '',
})

const defaultBackground = () => ({ type: 'color', value: '#061A2E' })
const defaultTheme = () => ({ mode: 'dark', primaryColor: '#00D6FF' })

const emptyForm = (): DashboardPayload => ({
  dashboardCode: '',
  name: '',
  description: '',
  screenWidth: 1920,
  screenHeight: 1080,
  backgroundConfigJson: defaultBackground(),
  themeConfigJson: defaultTheme(),
  enabled: false,
})

const items = ref<DashboardItem[]>([])
const activeId = ref('')
const draft = ref<DashboardDraft | null>(null)
const loading = ref(false)
const saving = ref(false)
const draftSaving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const form = reactive<DashboardPayload>(emptyForm())
const draftText = ref('')

const isEditing = computed(() => Boolean(activeId.value))

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listDashboards({
      name: filters.name || undefined,
      dashboardCode: filters.dashboardCode || undefined,
      status: filters.status || undefined,
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
  draft.value = null
  draftText.value = ''
  errorMessage.value = ''
  successMessage.value = ''
}

async function startEdit(item: DashboardItem) {
  activeId.value = item.id
  Object.assign(form, {
    dashboardCode: item.dashboardCode,
    name: item.name,
    description: item.description || '',
    screenWidth: item.screenWidth,
    screenHeight: item.screenHeight,
    backgroundConfigJson: item.backgroundConfigJson || defaultBackground(),
    themeConfigJson: item.themeConfigJson || defaultTheme(),
    enabled: item.enabled,
  })
  errorMessage.value = ''
  successMessage.value = ''
  await loadDraft(item.id)
}

async function saveForm() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (isEditing.value) {
      await updateDashboard(activeId.value, form)
      successMessage.value = '大屏基础信息已保存'
    } else {
      const response = await createDashboard(form)
      activeId.value = response.data.id
      successMessage.value = '大屏已创建，并已自动生成空草稿'
    }
    await loadList()
    if (activeId.value) {
      await loadDraft(activeId.value)
    }
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function toggleDashboard(item: DashboardItem, enabled: boolean) {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (enabled) {
      await enableDashboard(item.id)
      successMessage.value = '大屏已启用'
    } else {
      await disableDashboard(item.id)
      successMessage.value = '大屏已停用'
    }
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function loadDraft(id: string) {
  const response = await getDashboardDraft(id)
  draft.value = response.data
  draftText.value = JSON.stringify(response.data.configJson, null, 2)
}

async function saveDraft() {
  if (!activeId.value) {
    errorMessage.value = '请先创建或选择一个大屏'
    return
  }
  draftSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const parsed = parseJsonObject(draftText.value, '草稿 configJson')
    await saveDashboardDraft(activeId.value, parsed)
    await loadDraft(activeId.value)
    successMessage.value = '草稿已保存'
  } catch (error) {
    showError(error)
  } finally {
    draftSaving.value = false
  }
}

function applyBasicDraft() {
  const config = {
    schemaVersion: '1.0',
    canvas: {
      width: form.screenWidth,
      height: form.screenHeight,
      gridSize: 8,
      background: form.backgroundConfigJson,
    },
    theme: form.themeConfigJson,
    cards: [],
    interactions: [],
    aiContext: {
      enabled: false,
      manifestReserved: true,
    },
  }
  draftText.value = JSON.stringify(config, null, 2)
}

function updateColorDraft() {
  form.backgroundConfigJson = { type: 'color', value: backgroundColor.value }
  form.themeConfigJson = { mode: 'dark', primaryColor: primaryColor.value }
  if (draftText.value) {
    applyBasicDraft()
  }
}

const backgroundColor = computed({
  get: () => String(form.backgroundConfigJson.value || '#061A2E'),
  set: (value: string) => {
    form.backgroundConfigJson = { type: 'color', value }
  },
})

const primaryColor = computed({
  get: () => String(form.themeConfigJson.primaryColor || '#00D6FF'),
  set: (value: string) => {
    form.themeConfigJson = { mode: 'dark', primaryColor: value }
  },
})

function formatDraft() {
  try {
    draftText.value = JSON.stringify(parseJsonObject(draftText.value, '草稿 configJson'), null, 2)
    errorMessage.value = ''
  } catch (error) {
    showError(error)
  }
}

function parseJsonObject(value: string, fieldName: string): Record<string, unknown> {
  const parsed = JSON.parse(value)
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error(`${fieldName} 必须是 JSON 对象`)
  }
  return parsed as Record<string, unknown>
}

function showError(error: unknown) {
  if (error instanceof Error) {
    errorMessage.value = error.message
    return
  }
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
        <p class="eyebrow">DASHBOARD MANAGEMENT</p>
        <h2>大屏管理</h2>
        <p>管理大屏基础信息和空草稿配置。本阶段只保存大屏级配置，不开发拖拽画布和发布浏览。</p>
      </div>
      <button type="button" class="secondary-button" @click="startCreate">新增大屏</button>
    </div>

    <div class="filters-panel">
      <label>
        名称
        <input v-model="filters.name" placeholder="按名称筛选" />
      </label>
      <label>
        编码
        <input v-model="filters.dashboardCode" placeholder="按编码筛选" />
      </label>
      <label>
        状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
      <span></span>
      <button type="button" class="primary-button" :disabled="loading" @click="loadList">
        {{ loading ? '查询中...' : '查询' }}
      </button>
    </div>

    <p v-if="errorMessage" class="message error-message">{{ errorMessage }}</p>
    <p v-if="successMessage" class="message success-message">{{ successMessage }}</p>

    <div class="data-source-grid">
      <div class="table-panel">
        <table class="data-table">
          <thead>
            <tr>
              <th>大屏</th>
              <th>尺寸</th>
              <th>状态</th>
              <th>发布版本</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && items.length === 0">
              <td colspan="6" class="empty-cell">暂无大屏</td>
            </tr>
            <tr v-for="item in items" :key="item.id">
              <td>
                <strong>{{ item.name }}</strong>
                <span class="muted-line">{{ item.dashboardCode }}</span>
              </td>
              <td>{{ item.screenWidth }}×{{ item.screenHeight }}</td>
              <td>
                <span :class="['status-pill', item.enabled ? 'status-up' : 'status-down']">
                  {{ item.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ item.currentPublishedVersionId || '未发布' }}</td>
              <td>{{ formatTime(item.updatedAt) }}</td>
              <td class="actions-cell">
                <button type="button" @click="startEdit(item)">编辑/草稿</button>
                <button v-if="!item.enabled" type="button" @click="toggleDashboard(item, true)">启用</button>
                <button v-else type="button" class="danger-button" @click="toggleDashboard(item, false)">停用</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="form-stack">
        <form class="form-panel" @submit.prevent="saveForm">
          <h3>{{ isEditing ? '编辑大屏基础信息' : '新增大屏' }}</h3>
          <div class="form-grid">
            <label>
              大屏编码
              <input v-model.trim="form.dashboardCode" required placeholder="city_water_overview" />
            </label>
            <label>
              大屏名称
              <input v-model.trim="form.name" required placeholder="统一智慧水务总览" />
            </label>
            <label>
              画布宽度
              <input v-model.number="form.screenWidth" type="number" min="320" required @change="applyBasicDraft" />
            </label>
            <label>
              画布高度
              <input v-model.number="form.screenHeight" type="number" min="240" required @change="applyBasicDraft" />
            </label>
            <label>
              背景色
              <input v-model="backgroundColor" type="color" @change="updateColorDraft" />
            </label>
            <label>
              主题色
              <input v-model="primaryColor" type="color" @change="updateColorDraft" />
            </label>
            <label class="full-field">
              描述
              <textarea v-model.trim="form.description" rows="3" placeholder="大屏用途、适用场景或维护说明"></textarea>
            </label>
            <label class="checkbox-field">
              <input v-model="form.enabled" type="checkbox" />
              保存后启用
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="primary-button" :disabled="saving">
              {{ saving ? '保存中...' : '保存大屏' }}
            </button>
          </div>
        </form>

        <section class="form-panel">
          <h3>草稿基础配置</h3>
          <p class="muted-line">
            当前 revision：{{ draft?.revision ?? '-' }}；这里只编辑空画布配置，cards 保持为空数组。
          </p>
          <div class="form-actions">
            <button type="button" class="secondary-button" @click="applyBasicDraft">生成基础草稿</button>
            <button type="button" class="secondary-button" :disabled="!draftText" @click="formatDraft">格式化 JSON</button>
          </div>
          <label class="full-field">
            configJson
            <textarea v-model="draftText" rows="18" placeholder="创建或选择大屏后自动加载草稿"></textarea>
          </label>
          <div class="form-actions">
            <button type="button" class="primary-button" :disabled="draftSaving || !activeId" @click="saveDraft">
              {{ draftSaving ? '保存中...' : '保存草稿' }}
            </button>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

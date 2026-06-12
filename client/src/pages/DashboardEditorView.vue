<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { getDashboardDraft, listDashboards, saveDashboardDraft, type DashboardDraft, type DashboardItem } from '@/api/dashboards'
import { listComponentTemplates, type ComponentTemplateItem } from '@/api/componentTemplates'
import type { RequestError } from '@/api/request'

interface DraftConfig {
  schemaVersion: string
  canvas: {
    width: number
    height: number
    gridSize: number
    background: {
      type: string
      value: string
    }
  }
  theme: {
    mode: string
    primaryColor: string
  }
  cards: unknown[]
  interactions: unknown[]
  aiContext: {
    enabled: boolean
    manifestReserved: boolean
  }
}

const route = useRoute()
const dashboardId = computed(() => String(route.params.id || ''))

const dashboard = ref<DashboardItem | null>(null)
const draft = ref<DashboardDraft | null>(null)
const templates = ref<ComponentTemplateItem[]>([])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const editorConfig = reactive<DraftConfig>({
  schemaVersion: '1.0',
  canvas: {
    width: 1920,
    height: 1080,
    gridSize: 8,
    background: {
      type: 'color',
      value: '#061A2E',
    },
  },
  theme: {
    mode: 'dark',
    primaryColor: '#00D6FF',
  },
  cards: [],
  interactions: [],
  aiContext: {
    enabled: false,
    manifestReserved: true,
  },
})

const canvasPreviewStyle = computed(() => {
  const maxWidth = 980
  const width = Number(editorConfig.canvas.width) || 1920
  const height = Number(editorConfig.canvas.height) || 1080
  const scale = Math.min(1, maxWidth / width)
  return {
    width: `${Math.max(320, width * scale)}px`,
    height: `${Math.max(180, height * scale)}px`,
    backgroundColor: editorConfig.canvas.background.value || '#061A2E',
    borderColor: editorConfig.theme.primaryColor || '#00D6FF',
  }
})

async function loadEditor() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const [dashboardResponse, draftResponse, templateResponse] = await Promise.all([
      listDashboards({}),
      getDashboardDraft(dashboardId.value),
      listComponentTemplates({ status: 'ENABLED' }),
    ])
    dashboard.value = dashboardResponse.data.items.find((item) => item.id === dashboardId.value) || null
    draft.value = draftResponse.data
    templates.value = templateResponse.data.items
    applyDraftConfig(draftResponse.data.configJson)
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

function applyDraftConfig(rawConfig: Record<string, unknown>) {
  const config = normalizeDraft(rawConfig)
  editorConfig.schemaVersion = config.schemaVersion
  editorConfig.canvas.width = config.canvas.width
  editorConfig.canvas.height = config.canvas.height
  editorConfig.canvas.gridSize = config.canvas.gridSize
  editorConfig.canvas.background = config.canvas.background
  editorConfig.theme = config.theme
  editorConfig.cards = config.cards
  editorConfig.interactions = config.interactions
  editorConfig.aiContext = config.aiContext
}

async function saveDraft() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const payload = normalizeDraft(editorConfig)
    const response = await saveDashboardDraft(dashboardId.value, payload as unknown as Record<string, unknown>)
    draft.value = response.data
    applyDraftConfig(response.data.configJson)
    successMessage.value = `草稿已保存，revision=${response.data.revision}`
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

function normalizeDraft(rawConfig: Record<string, unknown>): DraftConfig {
  const source = rawConfig as Partial<DraftConfig>
  const canvas = (source.canvas || {}) as Partial<DraftConfig['canvas']>
  const background = (canvas.background || { type: 'color', value: '#061A2E' }) as Partial<
    DraftConfig['canvas']['background']
  >
  const theme = (source.theme || { mode: 'dark', primaryColor: '#00D6FF' }) as Partial<DraftConfig['theme']>
  return {
    schemaVersion: source.schemaVersion || '1.0',
    canvas: {
      width: toPositiveNumber(canvas.width, 1920),
      height: toPositiveNumber(canvas.height, 1080),
      gridSize: toPositiveNumber(canvas.gridSize, 8),
      background: {
        type: background.type || 'color',
        value: background.value || '#061A2E',
      },
    },
    theme: {
      mode: theme.mode || 'dark',
      primaryColor: theme.primaryColor || '#00D6FF',
    },
    cards: Array.isArray(source.cards) ? source.cards : [],
    interactions: Array.isArray(source.interactions) ? source.interactions : [],
    aiContext: {
      enabled: Boolean(source.aiContext?.enabled),
      manifestReserved: source.aiContext?.manifestReserved !== false,
    },
  }
}

function toPositiveNumber(value: unknown, fallback: number) {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : fallback
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

function renderEngineLabel(value: string) {
  const labels: Record<string, string> = {
    TEXT_CARD: '图文指标卡',
    ECHARTS_LINE: '折线图卡',
    ECHARTS_BAR: '柱状图卡',
    TABLE_LIST: '列表表格卡',
    OPENLAYERS_MAP: '地图卡预留',
    BABYLON_SCENE: '三维卡预留',
    G6_TOPOLOGY: '拓扑卡预留',
  }
  return labels[value] || value
}

onMounted(loadEditor)
</script>

<template>
  <section class="editor-shell">
    <header class="editor-toolbar">
      <RouterLink class="secondary-button" to="/dashboards">返回大屏列表</RouterLink>
      <div class="editor-title">
        <p class="eyebrow">DASHBOARD EDITOR</p>
        <h2>{{ dashboard?.name || '大屏编辑器' }}</h2>
        <span>revision：{{ draft?.revision ?? '-' }}</span>
      </div>
      <div class="editor-actions">
        <button type="button" class="primary-button" :disabled="saving || loading" @click="saveDraft">
          {{ saving ? '保存中...' : '保存草稿' }}
        </button>
        <button type="button" class="secondary-button" disabled>预览：后续阶段开放</button>
        <button type="button" class="secondary-button" disabled>发布：后续阶段开放</button>
      </div>
    </header>

    <p v-if="errorMessage" class="message error-message">{{ errorMessage }}</p>
    <p v-if="successMessage" class="message success-message">{{ successMessage }}</p>

    <div class="editor-layout">
      <aside class="editor-panel template-library">
        <h3>组件模板库</h3>
        <p class="muted-line">仅展示已启用模板，本阶段不做拖拽放置。</p>
        <article v-if="templates.length === 0" class="empty-cell">暂无启用组件模板</article>
        <article v-for="template in templates" :key="template.id" class="template-card">
          <strong>{{ template.name }}</strong>
          <span>{{ template.category }} / {{ renderEngineLabel(template.renderEngine) }}</span>
          <small>{{ template.templateCode }}</small>
        </article>
      </aside>

      <main class="canvas-stage">
        <div class="canvas-meta">
          <span>{{ editorConfig.canvas.width }} × {{ editorConfig.canvas.height }}</span>
          <span>grid {{ editorConfig.canvas.gridSize }}</span>
          <span>cards {{ editorConfig.cards.length }}</span>
        </div>
        <div class="canvas-viewport">
          <div class="empty-canvas" :style="canvasPreviewStyle">
            <div v-if="editorConfig.cards.length === 0" class="canvas-empty-state">
              <strong>暂无卡片</strong>
              <span>请从左侧组件模板库添加。真实拖拽与卡片实例将在后续阶段开放。</span>
            </div>
          </div>
        </div>
      </main>

      <aside class="editor-panel property-panel">
        <h3>基础属性</h3>
        <label>
          画布宽度
          <input v-model.number="editorConfig.canvas.width" type="number" min="320" />
        </label>
        <label>
          画布高度
          <input v-model.number="editorConfig.canvas.height" type="number" min="240" />
        </label>
        <label>
          背景色
          <input v-model="editorConfig.canvas.background.value" type="color" />
        </label>
        <label>
          主题色
          <input v-model="editorConfig.theme.primaryColor" type="color" />
        </label>
        <label class="checkbox-field">
          <input v-model="editorConfig.aiContext.enabled" type="checkbox" />
          AI Context 预留开关
        </label>
        <p class="muted-line">当前仅保存草稿配置，不调用 AI Gateway，不生成发布 Manifest。</p>
      </aside>
    </div>
  </section>
</template>

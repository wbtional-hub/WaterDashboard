<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import {
  createDashboardCard,
  deleteDashboardCard,
  getDashboardDraft,
  listDashboardCards,
  listDashboards,
  previewDashboardCardQuery,
  saveDashboardDraft,
  updateDashboardCard,
  type DashboardCardPreviewResult,
  type DashboardCardItem,
  type DashboardDraft,
  type DashboardItem,
} from '@/api/dashboards'
import { listComponentTemplates, type ComponentTemplateItem } from '@/api/componentTemplates'
import { listDataSources, type DataSourceItem } from '@/api/dataSources'
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

interface CardDataBinding {
  enabled: boolean
  dataSourceId: string
  queryType: 'SQL'
  sql: string
  params: Record<string, unknown>
  fieldMapping: Record<string, unknown>
}

interface CardRefresh {
  enabled: boolean
  intervalSeconds: number
}

const route = useRoute()
const dashboardId = computed(() => String(route.params.id || ''))

const dashboard = ref<DashboardItem | null>(null)
const draft = ref<DashboardDraft | null>(null)
const templates = ref<ComponentTemplateItem[]>([])
const dataSources = ref<DataSourceItem[]>([])
const cards = ref<DashboardCardItem[]>([])
const selectedCardId = ref('')
const fieldMappingText = ref('{}')
const previewing = ref(false)
const previewResult = ref<DashboardCardPreviewResult | null>(null)
const previewError = ref('')
const previewTraceId = ref('')
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

const selectedCard = computed(() => cards.value.find((card) => card.cardId === selectedCardId.value) || null)
const enabledDataSources = computed(() => dataSources.value.filter((item) => item.enabled && item.status === 'ENABLED'))
const selectedDataBinding = computed<CardDataBinding>(() => ensureSelectedCardDataBinding())
const selectedRefresh = computed<CardRefresh>(() => ensureSelectedCardRefresh())
const selectedBoundDataSource = computed(() => {
  const dataSourceId = selectedDataBinding.value.dataSourceId
  return dataSources.value.find((item) => item.id === dataSourceId) || null
})
const canPreviewSelectedCard = computed(() => {
  const binding = selectedDataBinding.value
  return Boolean(selectedCard.value && binding.enabled && binding.dataSourceId && binding.sql.trim() && !previewing.value)
})

async function loadEditor() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const [dashboardResponse, draftResponse, templateResponse, cardResponse, dataSourceResponse] = await Promise.all([
      listDashboards({}),
      getDashboardDraft(dashboardId.value),
      listComponentTemplates({ status: 'ENABLED' }),
      listDashboardCards(dashboardId.value),
      listDataSources({}),
    ])
    dashboard.value = dashboardResponse.data.items.find((item) => item.id === dashboardId.value) || null
    draft.value = draftResponse.data
    templates.value = templateResponse.data.items
    cards.value = cardResponse.data
    dataSources.value = dataSourceResponse.data.items
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

async function addTemplate(template: ComponentTemplateItem) {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const offset = cards.value.length * 24
    const response = await createDashboardCard(dashboardId.value, {
      title: template.name,
      templateId: template.id,
      x: 40 + offset,
      y: 40 + offset,
      enabled: true,
      aiEnabled: false,
      configJson: {},
    })
    await reloadDraftAndCards()
    selectedCardId.value = response.data.cardId
    successMessage.value = '卡片已添加到草稿'
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function saveSelectedCard() {
  if (!selectedCard.value) {
    return
  }
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const card = selectedCard.value
    const fieldMapping = parseFieldMapping()
    const dataBinding = ensureSelectedCardDataBinding()
    dataBinding.fieldMapping = fieldMapping
    validateCardDataBinding(dataBinding)
    const refresh = ensureSelectedCardRefresh()
    card.configJson = {
      ...(card.configJson || {}),
      dataBinding,
      refresh,
    }
    await updateDashboardCard(dashboardId.value, card.cardId, {
      cardCode: card.cardCode,
      title: card.title,
      x: card.x,
      y: card.y,
      width: card.width,
      height: card.height,
      enabled: card.enabled,
      aiEnabled: card.aiEnabled,
      configJson: card.configJson || {},
    })
    await reloadDraftAndCards()
    selectedCardId.value = card.cardId
    successMessage.value = '卡片属性已保存'
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function removeSelectedCard() {
  if (!selectedCard.value || !window.confirm(`确认删除卡片“${selectedCard.value.title}”？`)) {
    return
  }
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const cardId = selectedCard.value.cardId
    await deleteDashboardCard(dashboardId.value, cardId)
    selectedCardId.value = ''
    await reloadDraftAndCards()
    successMessage.value = '卡片已删除'
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function previewSelectedCardQuery() {
  if (!selectedCard.value) {
    return
  }
  previewing.value = true
  previewResult.value = null
  previewError.value = ''
  previewTraceId.value = ''
  try {
    const binding = ensureSelectedCardDataBinding()
    if (!binding.enabled) {
      throw new Error('请先启用数据绑定')
    }
    validateCardDataBinding(binding)
    const response = await previewDashboardCardQuery(dashboardId.value, selectedCard.value.cardId, 20)
    previewResult.value = response.data
    previewTraceId.value = response.traceId
  } catch (error) {
    if (error instanceof Error) {
      previewError.value = error.message
      return
    }
    const requestError = error as RequestError
    previewError.value = requestError.message || 'SQL 预览失败，请稍后重试'
    previewTraceId.value = requestError.traceId || ''
  } finally {
    previewing.value = false
  }
}

async function reloadDraftAndCards() {
  const [draftResponse, cardResponse] = await Promise.all([
    getDashboardDraft(dashboardId.value),
    listDashboardCards(dashboardId.value),
  ])
  draft.value = draftResponse.data
  cards.value = cardResponse.data
  applyDraftConfig(draftResponse.data.configJson)
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

function ensureSelectedCardDataBinding(): CardDataBinding {
  const card = selectedCard.value
  const config = ensureSelectedCardConfig()
  const source = ((config.dataBinding || {}) as Partial<CardDataBinding>) || {}
  const binding: CardDataBinding = {
    enabled: Boolean(source.enabled),
    dataSourceId: typeof source.dataSourceId === 'string' ? source.dataSourceId : '',
    queryType: 'SQL',
    sql: typeof source.sql === 'string' ? source.sql : '',
    params: isPlainObject(source.params) ? source.params : {},
    fieldMapping: isPlainObject(source.fieldMapping) ? source.fieldMapping : {},
  }
  if (card) {
    config.dataBinding = binding as unknown as Record<string, unknown>
  }
  return binding
}

function ensureSelectedCardRefresh(): CardRefresh {
  const config = ensureSelectedCardConfig()
  const source = ((config.refresh || {}) as Partial<CardRefresh>) || {}
  const refresh: CardRefresh = {
    enabled: Boolean(source.enabled),
    intervalSeconds: toPositiveNumber(source.intervalSeconds, 60),
  }
  config.refresh = refresh as unknown as Record<string, unknown>
  return refresh
}

function ensureSelectedCardConfig() {
  const card = selectedCard.value
  if (!card) {
    return {}
  }
  if (!card.configJson || typeof card.configJson !== 'object') {
    card.configJson = {}
  }
  return card.configJson as Record<string, unknown>
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

function parseFieldMapping() {
  try {
    const parsed = JSON.parse(fieldMappingText.value || '{}') as unknown
    if (!isPlainObject(parsed)) {
      throw new Error('字段映射必须是 JSON 对象')
    }
    return parsed
  } catch (error) {
    throw new Error(error instanceof Error ? error.message : '字段映射 JSON 格式不正确')
  }
}

function validateCardDataBinding(binding: CardDataBinding) {
  const sql = binding.sql.trim()
  binding.sql = sql
  if (!binding.enabled && !sql) {
    return
  }
  if (binding.enabled && !binding.dataSourceId) {
    throw new Error('启用数据绑定时必须选择数据源')
  }
  if (binding.dataSourceId) {
    const dataSource = dataSources.value.find((item) => item.id === binding.dataSourceId)
    if (!dataSource || !dataSource.enabled || dataSource.status !== 'ENABLED') {
      throw new Error('当前绑定的数据源不可用，请选择已启用的数据源')
    }
  }
  if (!sql) {
    throw new Error('SQL 不能为空')
  }
  const upper = sql.toUpperCase()
  if (!(upper.startsWith('SELECT') || upper.startsWith('WITH'))) {
    throw new Error('卡片 SQL 只允许以 SELECT 或 WITH 开头')
  }
  if (sql.includes(';')) {
    throw new Error('卡片 SQL 不允许包含分号或多语句')
  }
  const dangerousKeywords = ['INSERT', 'UPDATE', 'DELETE', 'DROP', 'ALTER', 'TRUNCATE', 'CREATE', 'GRANT', 'REVOKE', 'EXECUTE', 'CALL']
  if (dangerousKeywords.some((keyword) => new RegExp(`\\b${keyword}\\b`, 'i').test(sql))) {
    throw new Error('卡片 SQL 包含不允许的高风险关键字')
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

function syncFieldMappingText() {
  if (!selectedCard.value) {
    fieldMappingText.value = '{}'
    return
  }
  fieldMappingText.value = JSON.stringify(ensureSelectedCardDataBinding().fieldMapping || {}, null, 2)
}

function resetPreviewState() {
  previewResult.value = null
  previewError.value = ''
  previewTraceId.value = ''
}

function displayCell(value: unknown) {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

watch(selectedCardId, () => {
  syncFieldMappingText()
  resetPreviewState()
})
watch(cards, syncFieldMappingText)

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
          <button type="button" class="secondary-button" :disabled="saving" @click="addTemplate(template)">
            添加到画布
          </button>
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
            <button
              v-for="card in cards"
              :key="card.cardId"
              type="button"
              :class="['canvas-card', selectedCardId === card.cardId ? 'canvas-card-selected' : '']"
              :style="{
                left: `${card.x}px`,
                top: `${card.y}px`,
                width: `${card.width}px`,
                height: `${card.height}px`,
              }"
              @click="selectedCardId = card.cardId"
            >
              <strong>{{ card.title }}</strong>
              <span>{{ card.templateCode }} / {{ card.renderEngine }}</span>
              <small>{{ card.aiEnabled ? 'AI 已启用' : 'AI 未启用' }}</small>
            </button>
            <div v-if="cards.length === 0" class="canvas-empty-state">
              <strong>暂无卡片</strong>
              <span>请从左侧组件模板库点击添加。复杂拖拽与数据绑定将在后续阶段开放。</span>
            </div>
          </div>
        </div>
      </main>

      <aside class="editor-panel property-panel">
        <template v-if="!selectedCard">
          <h3>画布属性</h3>
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
        </template>

        <template v-else>
          <h3>卡片属性</h3>
          <div class="property-group">
            <h4>基础配置</h4>
            <label>
              卡片标题
              <input v-model.trim="selectedCard.title" />
            </label>
            <label class="checkbox-field">
              <input v-model="selectedCard.enabled" type="checkbox" />
              启用卡片
            </label>
            <label class="checkbox-field">
              <input v-model="selectedCard.aiEnabled" type="checkbox" />
              AI 启用预留
            </label>
          </div>

          <div class="property-group">
            <h4>布局配置</h4>
            <label>
              X
              <input v-model.number="selectedCard.x" type="number" min="0" />
            </label>
            <label>
              Y
              <input v-model.number="selectedCard.y" type="number" min="0" />
            </label>
            <label>
              宽度
              <input v-model.number="selectedCard.width" type="number" min="80" />
            </label>
            <label>
              高度
              <input v-model.number="selectedCard.height" type="number" min="60" />
            </label>
          </div>

          <div class="property-group">
            <h4>数据配置</h4>
            <label class="checkbox-field">
              <input v-model="selectedDataBinding.enabled" type="checkbox" />
              启用数据绑定
            </label>
            <label>
              数据源
              <select v-model="selectedDataBinding.dataSourceId">
                <option value="">请选择已启用数据源</option>
                <option v-for="dataSource in enabledDataSources" :key="dataSource.id" :value="dataSource.id">
                  {{ dataSource.name }} / {{ dataSource.code || '-' }} / {{ dataSource.type }} /
                  {{ dataSource.environment || '-' }} / {{ dataSource.status }}
                </option>
              </select>
            </label>
            <p v-if="selectedDataBinding.dataSourceId && (!selectedBoundDataSource || !selectedBoundDataSource.enabled)" class="message error-message compact-message">
              当前卡片已绑定的数据源不可用，请重新选择已启用数据源后保存。
            </p>
            <label>
              SELECT SQL
              <textarea
                v-model.trim="selectedDataBinding.sql"
                rows="8"
                placeholder="SELECT ...，本阶段仅保存配置，不执行 SQL"
              />
            </label>
            <label>
              字段映射 JSON 预留
              <textarea v-model="fieldMappingText" rows="6" placeholder='{"value":"total"}' />
            </label>
            <button
              type="button"
              class="secondary-button"
              :disabled="!canPreviewSelectedCard"
              @click="previewSelectedCardQuery"
            >
              {{ previewing ? 'SQL 预览中...' : 'SQL 预览' }}
            </button>
            <p class="muted-line">预览只返回少量样例数据，不保存到草稿，不做字段自动识别。</p>
            <p v-if="previewError" class="message error-message compact-message">
              {{ previewError }}<span v-if="previewTraceId">（traceId: {{ previewTraceId }}）</span>
            </p>
            <div v-if="previewResult" class="preview-result">
              <div class="preview-meta">
                <span>耗时：{{ previewResult.durationMs }} ms</span>
                <span>行数：{{ previewResult.rowCount }}</span>
                <span>traceId：{{ previewTraceId || '-' }}</span>
              </div>
              <p class="muted-line">字段映射下一阶段开放；当前可参考字段名手工填写 fieldMapping JSON。</p>
              <div class="preview-table-wrap">
                <table class="preview-table">
                  <thead>
                    <tr>
                      <th v-for="column in previewResult.columns" :key="column.name">
                        {{ column.name }}
                        <small>{{ column.type }}</small>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, rowIndex) in previewResult.rows" :key="rowIndex">
                      <td v-for="column in previewResult.columns" :key="column.name">
                        {{ displayCell(row[column.name]) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="property-group">
            <h4>刷新配置</h4>
            <label class="checkbox-field">
              <input v-model="selectedRefresh.enabled" type="checkbox" />
              自动刷新
            </label>
            <label>
              刷新间隔秒数
              <input v-model.number="selectedRefresh.intervalSeconds" type="number" min="5" max="86400" />
            </label>
          </div>

          <button type="button" class="primary-button" :disabled="saving" @click="saveSelectedCard">保存卡片配置</button>
          <button type="button" class="danger-button" :disabled="saving" @click="removeSelectedCard">删除卡片</button>
          <button type="button" class="secondary-button" @click="selectedCardId = ''">返回画布属性</button>
        </template>
      </aside>
    </div>
  </section>
</template>

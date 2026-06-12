<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createComponentTemplate,
  createComponentTemplateVersion,
  disableComponentTemplate,
  enableComponentTemplate,
  getComponentTemplate,
  listComponentTemplates,
  updateComponentTemplate,
  type ComponentTemplateItem,
  type ComponentTemplatePayload,
  type ComponentTemplateVersion,
  type ComponentTemplateVersionPayload,
} from '@/api/componentTemplates'
import type { RequestError } from '@/api/request'

const renderEngines = [
  { value: 'TEXT_CARD', label: '图文指标卡' },
  { value: 'ECHARTS_LINE', label: '折线图卡' },
  { value: 'ECHARTS_BAR', label: '柱状图卡' },
  { value: 'TABLE_LIST', label: '列表表格卡' },
  { value: 'OPENLAYERS_MAP', label: '地图卡预留' },
  { value: 'BABYLON_SCENE', label: '三维卡预留' },
  { value: 'G6_TOPOLOGY', label: '拓扑卡预留' },
]

const filters = reactive({
  name: '',
  templateCode: '',
  category: '',
  status: '',
})

const emptyTemplateForm = (): ComponentTemplatePayload => ({
  templateCode: '',
  name: '',
  category: 'basic',
  renderEngine: 'TEXT_CARD',
  description: '',
  minWidth: 1,
  minHeight: 1,
  defaultWidth: 4,
  defaultHeight: 3,
  licenseScope: '',
  signature: '',
  checksum: '',
  enabled: false,
})

const emptyVersionForm = () => ({
  version: '1.0.0',
  dataContractJson: JSON.stringify(
    {
      fields: [{ name: 'value', type: 'number', label: '指标值', required: true }],
    },
    null,
    2,
  ),
  defaultConfigJson: JSON.stringify(
    {
      title: '组件标题',
      refreshIntervalSeconds: 60,
    },
    null,
    2,
  ),
  fieldMappingSchemaJson: JSON.stringify(
    {
      mappings: [{ target: 'value', sourceField: '', required: true }],
    },
    null,
    2,
  ),
  checksum: '',
})

const items = ref<ComponentTemplateItem[]>([])
const versions = ref<ComponentTemplateVersion[]>([])
const loading = ref(false)
const saving = ref(false)
const versionSaving = ref(false)
const activeId = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const form = reactive<ComponentTemplatePayload>(emptyTemplateForm())
const versionForm = reactive(emptyVersionForm())

const isEditing = computed(() => Boolean(activeId.value))

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listComponentTemplates({
      name: filters.name || undefined,
      templateCode: filters.templateCode || undefined,
      category: filters.category || undefined,
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
  Object.assign(form, emptyTemplateForm())
  Object.assign(versionForm, emptyVersionForm())
  activeId.value = ''
  versions.value = []
  errorMessage.value = ''
  successMessage.value = ''
}

async function startEdit(item: ComponentTemplateItem) {
  activeId.value = item.id
  Object.assign(form, {
    templateCode: item.templateCode,
    name: item.name,
    category: item.category,
    renderEngine: item.renderEngine,
    description: item.description || '',
    minWidth: item.minWidth,
    minHeight: item.minHeight,
    defaultWidth: item.defaultWidth,
    defaultHeight: item.defaultHeight,
    licenseScope: item.licenseScope || '',
    signature: item.signature || '',
    checksum: item.checksum || '',
    enabled: item.enabled,
  })
  Object.assign(versionForm, emptyVersionForm())
  errorMessage.value = ''
  successMessage.value = ''
  await loadVersions(item.id)
}

async function saveTemplate() {
  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (isEditing.value) {
      await updateComponentTemplate(activeId.value, form)
      successMessage.value = '组件模板已保存'
    } else {
      const response = await createComponentTemplate(form)
      activeId.value = response.data.id
      successMessage.value = '组件模板已创建'
    }
    await loadList()
    if (activeId.value) {
      await loadVersions(activeId.value)
    }
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function toggleTemplate(item: ComponentTemplateItem, enabled: boolean) {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    if (enabled) {
      await enableComponentTemplate(item.id)
      successMessage.value = '组件模板已启用'
    } else {
      await disableComponentTemplate(item.id)
      successMessage.value = '组件模板已停用'
    }
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    loading.value = false
  }
}

async function loadVersions(id: string) {
  const response = await getComponentTemplate(id)
  versions.value = response.data.versions
}

async function saveVersion() {
  if (!activeId.value) {
    errorMessage.value = '请先创建或选择一个组件模板'
    return
  }
  versionSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const payload: ComponentTemplateVersionPayload = {
      version: versionForm.version,
      dataContractJson: parseJson(versionForm.dataContractJson, 'dataContractJson'),
      defaultConfigJson: parseJson(versionForm.defaultConfigJson, 'defaultConfigJson'),
      fieldMappingSchemaJson: parseJson(versionForm.fieldMappingSchemaJson, 'fieldMappingSchemaJson'),
      checksum: versionForm.checksum,
    }
    await createComponentTemplateVersion(activeId.value, payload)
    successMessage.value = '组件模板版本已创建'
    Object.assign(versionForm, emptyVersionForm())
    await loadVersions(activeId.value)
    await loadList()
  } catch (error) {
    showError(error)
  } finally {
    versionSaving.value = false
  }
}

function formatJsonField(field: 'dataContractJson' | 'defaultConfigJson' | 'fieldMappingSchemaJson') {
  try {
    versionForm[field] = JSON.stringify(parseJson(versionForm[field], field), null, 2)
    errorMessage.value = ''
  } catch (error) {
    showError(error)
  }
}

function parseJson(value: string, fieldName: string): Record<string, unknown> {
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

function renderEngineLabel(value: string) {
  return renderEngines.find((item) => item.value === value)?.label ?? value
}

onMounted(loadList)
</script>

<template>
  <section class="page-card">
    <div class="page-heading">
      <div>
        <p class="eyebrow">COMPONENT TEMPLATE LIBRARY</p>
        <h2>组件模板管理</h2>
        <p>维护组件模板元数据、渲染引擎类型和版本契约，为后续大屏编辑器模板库做准备。</p>
      </div>
      <button type="button" class="secondary-button" @click="startCreate">新增模板</button>
    </div>

    <div class="filters-panel">
      <label>
        名称
        <input v-model="filters.name" placeholder="按名称筛选" />
      </label>
      <label>
        编码
        <input v-model="filters.templateCode" placeholder="按编码筛选" />
      </label>
      <label>
        分类
        <input v-model="filters.category" placeholder="basic / chart / map" />
      </label>
      <label>
        状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
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
              <th>模板</th>
              <th>分类</th>
              <th>渲染引擎</th>
              <th>版本</th>
              <th>尺寸</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && items.length === 0">
              <td colspan="8" class="empty-cell">暂无组件模板</td>
            </tr>
            <tr v-for="item in items" :key="item.id">
              <td>
                <strong>{{ item.name }}</strong>
                <span class="muted-line">{{ item.templateCode }}</span>
              </td>
              <td>{{ item.category }}</td>
              <td>
                {{ renderEngineLabel(item.renderEngine) }}
                <span class="muted-line">{{ item.renderEngine }}</span>
              </td>
              <td>{{ item.currentVersion || '-' }}</td>
              <td>{{ item.defaultWidth }}×{{ item.defaultHeight }}</td>
              <td>
                <span :class="['status-pill', item.enabled ? 'status-up' : 'status-down']">
                  {{ item.enabled ? '启用' : '停用' }}
                </span>
              </td>
              <td>{{ formatTime(item.updatedAt) }}</td>
              <td class="actions-cell">
                <button type="button" @click="startEdit(item)">编辑</button>
                <button v-if="!item.enabled" type="button" @click="toggleTemplate(item, true)">启用</button>
                <button v-else type="button" class="danger-button" @click="toggleTemplate(item, false)">停用</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="form-stack">
        <form class="form-panel" @submit.prevent="saveTemplate">
          <h3>{{ isEditing ? '编辑组件模板' : '新增组件模板' }}</h3>
          <div class="form-grid">
            <label>
              模板编码
              <input v-model.trim="form.templateCode" required placeholder="text_metric_card" />
            </label>
            <label>
              模板名称
              <input v-model.trim="form.name" required placeholder="图文指标卡" />
            </label>
            <label>
              分类
              <input v-model.trim="form.category" required placeholder="basic" />
            </label>
            <label>
              渲染引擎
              <select v-model="form.renderEngine">
                <option v-for="engine in renderEngines" :key="engine.value" :value="engine.value">
                  {{ engine.label }}
                </option>
              </select>
            </label>
            <label>
              最小宽度
              <input v-model.number="form.minWidth" type="number" min="1" required />
            </label>
            <label>
              最小高度
              <input v-model.number="form.minHeight" type="number" min="1" required />
            </label>
            <label>
              默认宽度
              <input v-model.number="form.defaultWidth" type="number" min="1" required />
            </label>
            <label>
              默认高度
              <input v-model.number="form.defaultHeight" type="number" min="1" required />
            </label>
            <label class="full-field">
              描述
              <textarea v-model.trim="form.description" rows="3" placeholder="模板用途和数据契约说明"></textarea>
            </label>
            <label>
              License Scope 预留
              <input v-model.trim="form.licenseScope" placeholder="basic / pro" />
            </label>
            <label>
              Checksum 预留
              <input v-model.trim="form.checksum" placeholder="后续组件签名校验" />
            </label>
            <label class="full-field">
              Signature 预留
              <textarea v-model.trim="form.signature" rows="2" placeholder="后续非对称签名"></textarea>
            </label>
            <label class="checkbox-field">
              <input v-model="form.enabled" type="checkbox" />
              保存后启用
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="primary-button" :disabled="saving">
              {{ saving ? '保存中...' : '保存模板' }}
            </button>
          </div>
        </form>

        <form class="form-panel" @submit.prevent="saveVersion">
          <h3>创建模板版本</h3>
          <p class="muted-line">版本只新增不覆盖，用于追溯数据契约、默认配置和字段映射契约。</p>
          <div class="form-grid">
            <label>
              版本号
              <input v-model.trim="versionForm.version" required placeholder="1.0.0" />
            </label>
            <label>
              Checksum 预留
              <input v-model.trim="versionForm.checksum" placeholder="版本校验和" />
            </label>
            <label class="full-field">
              dataContractJson
              <textarea v-model="versionForm.dataContractJson" rows="8"></textarea>
              <button type="button" class="secondary-button" @click="formatJsonField('dataContractJson')">
                格式化
              </button>
            </label>
            <label class="full-field">
              defaultConfigJson
              <textarea v-model="versionForm.defaultConfigJson" rows="7"></textarea>
              <button type="button" class="secondary-button" @click="formatJsonField('defaultConfigJson')">
                格式化
              </button>
            </label>
            <label class="full-field">
              fieldMappingSchemaJson
              <textarea v-model="versionForm.fieldMappingSchemaJson" rows="7"></textarea>
              <button type="button" class="secondary-button" @click="formatJsonField('fieldMappingSchemaJson')">
                格式化
              </button>
            </label>
          </div>
          <div class="form-actions">
            <button type="submit" class="primary-button" :disabled="versionSaving || !activeId">
              {{ versionSaving ? '创建中...' : '创建版本' }}
            </button>
          </div>
        </form>

        <section class="form-panel">
          <h3>模板版本</h3>
          <div v-if="versions.length === 0" class="empty-cell">请选择模板或创建版本</div>
          <article v-for="version in versions" :key="version.id" class="version-card">
            <strong>{{ version.version }}</strong>
            <span>schemaVersion：{{ version.schemaVersion }}</span>
            <span>checksum：{{ version.checksum || '-' }}</span>
            <span>创建时间：{{ formatTime(version.createdAt) }}</span>
          </article>
        </section>
      </div>
    </div>
  </section>
</template>

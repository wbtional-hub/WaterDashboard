import request, { type ApiResponse } from './request'

export interface ComponentTemplateItem {
  id: string
  templateCode: string
  name: string
  category: string
  renderEngine: string
  currentVersion?: string
  status: string
  enabled: boolean
  description?: string
  minWidth: number
  minHeight: number
  defaultWidth: number
  defaultHeight: number
  licenseScope?: string
  signature?: string
  checksum?: string
  createdAt: string
  updatedAt: string
}

export interface ComponentTemplateListResponse {
  items: ComponentTemplateItem[]
  total: number
}

export interface ComponentTemplateDetailResponse {
  template: ComponentTemplateItem
  versions: ComponentTemplateVersion[]
}

export interface ComponentTemplatePayload {
  templateCode: string
  name: string
  category: string
  renderEngine: string
  description: string
  minWidth: number
  minHeight: number
  defaultWidth: number
  defaultHeight: number
  licenseScope: string
  signature: string
  checksum: string
  enabled: boolean
}

export interface ComponentTemplateVersion {
  id: string
  templateId: string
  version: string
  schemaVersion: string
  dataContractJson: Record<string, unknown>
  defaultConfigJson: Record<string, unknown>
  fieldMappingSchemaJson: Record<string, unknown>
  checksum?: string
  createdBy?: string
  createdAt: string
}

export interface ComponentTemplateVersionPayload {
  version: string
  dataContractJson: Record<string, unknown>
  defaultConfigJson: Record<string, unknown>
  fieldMappingSchemaJson: Record<string, unknown>
  checksum: string
}

export interface ComponentTemplateListParams {
  name?: string
  templateCode?: string
  category?: string
  status?: string
}

export async function listComponentTemplates(params: ComponentTemplateListParams) {
  const response = await request.get<ApiResponse<ComponentTemplateListResponse>>('/platform/component-templates', {
    params,
  })
  return response.data
}

export async function getComponentTemplate(id: string) {
  const response = await request.get<ApiResponse<ComponentTemplateDetailResponse>>(`/platform/component-templates/${id}`)
  return response.data
}

export async function createComponentTemplate(payload: ComponentTemplatePayload) {
  const response = await request.post<ApiResponse<ComponentTemplateItem>>('/platform/component-templates', payload)
  return response.data
}

export async function updateComponentTemplate(id: string, payload: ComponentTemplatePayload) {
  const response = await request.put<ApiResponse<ComponentTemplateItem>>(`/platform/component-templates/${id}`, payload)
  return response.data
}

export async function enableComponentTemplate(id: string) {
  const response = await request.patch<ApiResponse<ComponentTemplateItem>>(`/platform/component-templates/${id}/enable`)
  return response.data
}

export async function disableComponentTemplate(id: string) {
  const response = await request.patch<ApiResponse<ComponentTemplateItem>>(`/platform/component-templates/${id}/disable`)
  return response.data
}

export async function createComponentTemplateVersion(id: string, payload: ComponentTemplateVersionPayload) {
  const response = await request.post<ApiResponse<ComponentTemplateVersion>>(
    `/platform/component-templates/${id}/versions`,
    payload,
  )
  return response.data
}

export async function listComponentTemplateVersions(id: string) {
  const response = await request.get<ApiResponse<ComponentTemplateVersion[]>>(
    `/platform/component-templates/${id}/versions`,
  )
  return response.data
}

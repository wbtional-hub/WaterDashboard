import request, { type ApiResponse } from './request'

export interface DashboardItem {
  id: string
  dashboardCode: string
  name: string
  status: string
  enabled: boolean
  description?: string
  screenWidth: number
  screenHeight: number
  backgroundConfigJson: Record<string, unknown>
  themeConfigJson: Record<string, unknown>
  currentPublishedVersionId?: string
  createdAt: string
  updatedAt: string
}

export interface DashboardListResponse {
  items: DashboardItem[]
  total: number
}

export interface DashboardPayload {
  dashboardCode: string
  name: string
  description: string
  screenWidth: number
  screenHeight: number
  backgroundConfigJson: Record<string, unknown>
  themeConfigJson: Record<string, unknown>
  enabled: boolean
}

export interface DashboardDraft {
  id: string
  dashboardId: string
  revision: number
  schemaVersion: string
  configJson: Record<string, unknown>
  updatedBy?: string
  createdAt: string
  updatedAt: string
}

export interface DashboardCardPayload {
  cardCode?: string
  title?: string
  templateId?: string
  templateCode?: string
  templateVersionId?: string | null
  x?: number
  y?: number
  width?: number
  height?: number
  enabled?: boolean
  aiEnabled?: boolean
  configJson?: Record<string, unknown>
}

export interface DashboardCardItem {
  cardId: string
  dashboardId: string
  cardCode: string
  title: string
  templateCode: string
  templateId?: string
  templateVersionId?: string | null
  renderEngine: string
  x: number
  y: number
  width: number
  height: number
  enabled: boolean
  aiEnabled: boolean
  configJson: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface DashboardListParams {
  name?: string
  dashboardCode?: string
  status?: string
}

export async function listDashboards(params: DashboardListParams) {
  const response = await request.get<ApiResponse<DashboardListResponse>>('/platform/dashboards', { params })
  return response.data
}

export async function createDashboard(payload: DashboardPayload) {
  const response = await request.post<ApiResponse<DashboardItem>>('/platform/dashboards', payload)
  return response.data
}

export async function updateDashboard(id: string, payload: DashboardPayload) {
  const response = await request.put<ApiResponse<DashboardItem>>(`/platform/dashboards/${id}`, payload)
  return response.data
}

export async function enableDashboard(id: string) {
  const response = await request.patch<ApiResponse<DashboardItem>>(`/platform/dashboards/${id}/enable`)
  return response.data
}

export async function disableDashboard(id: string) {
  const response = await request.patch<ApiResponse<DashboardItem>>(`/platform/dashboards/${id}/disable`)
  return response.data
}

export async function getDashboardDraft(id: string) {
  const response = await request.get<ApiResponse<DashboardDraft>>(`/platform/dashboards/${id}/draft`)
  return response.data
}

export async function saveDashboardDraft(id: string, configJson: Record<string, unknown>) {
  const response = await request.put<ApiResponse<DashboardDraft>>(`/platform/dashboards/${id}/draft`, { configJson })
  return response.data
}

export async function listDashboardCards(id: string) {
  const response = await request.get<ApiResponse<DashboardCardItem[]>>(`/platform/dashboards/${id}/draft/cards`)
  return response.data
}

export async function createDashboardCard(id: string, payload: DashboardCardPayload) {
  const response = await request.post<ApiResponse<DashboardCardItem>>(`/platform/dashboards/${id}/draft/cards`, payload)
  return response.data
}

export async function updateDashboardCard(id: string, cardId: string, payload: DashboardCardPayload) {
  const response = await request.put<ApiResponse<DashboardCardItem>>(
    `/platform/dashboards/${id}/draft/cards/${cardId}`,
    payload,
  )
  return response.data
}

export async function deleteDashboardCard(id: string, cardId: string) {
  const response = await request.delete<ApiResponse<null>>(`/platform/dashboards/${id}/draft/cards/${cardId}`)
  return response.data
}

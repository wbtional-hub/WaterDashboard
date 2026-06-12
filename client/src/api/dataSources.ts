import request, { type ApiResponse } from './request'

export interface DataSourceItem {
  id: string
  code?: string
  name: string
  type: string
  environment?: string
  host: string
  port: number
  database: string
  username: string
  schema: string
  remark: string
  status: string
  enabled: boolean
  lastTestTime?: string
  createdAt: string
  updatedAt: string
}

export interface DataSourceListResponse {
  items: DataSourceItem[]
  total: number
}

export interface DataSourcePayload {
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  password?: string
  schema: string
  remark: string
  enabled: boolean
}

export interface DataSourceTestResult {
  success: boolean
  code: string
  message: string
  traceId: string
  latencyMs?: number
  databaseType?: string
  databaseVersion?: string
  errorSummary?: string
}

export interface DataSourceListParams {
  name?: string
  type?: string
  status?: string
  enabled?: boolean | ''
}

export async function listDataSources(params: DataSourceListParams) {
  const response = await request.get<ApiResponse<DataSourceListResponse>>('/platform/data-sources', { params })
  return response.data
}

export async function createDataSource(payload: DataSourcePayload) {
  const response = await request.post<ApiResponse<DataSourceItem>>('/platform/data-sources', payload)
  return response.data
}

export async function updateDataSource(id: string, payload: DataSourcePayload) {
  const response = await request.put<ApiResponse<DataSourceItem>>(`/platform/data-sources/${id}`, payload)
  return response.data
}

export async function disableDataSource(id: string) {
  const response = await request.patch<ApiResponse<DataSourceItem>>(`/platform/data-sources/${id}/disable`)
  return response.data
}

export async function testDataSource(id: string) {
  const response = await request.post<ApiResponse<DataSourceTestResult>>(`/platform/data-sources/${id}/test`)
  return response.data
}

export async function testTempDataSource(payload: DataSourcePayload) {
  const response = await request.post<ApiResponse<DataSourceTestResult>>('/platform/data-sources/test-temp', payload)
  return response.data
}

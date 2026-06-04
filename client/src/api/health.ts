import request, { type ApiResponse } from './request'

export interface HealthData {
  status: string
  currentTime: string
  applicationName: string
  version: string
}

export async function getHealth(): Promise<ApiResponse<HealthData>> {
  const response = await request.get<ApiResponse<HealthData>>('/health')
  return response.data
}


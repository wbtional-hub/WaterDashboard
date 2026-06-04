import axios, { AxiosError } from 'axios'

export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  traceId: string
}

export interface RequestError {
  message: string
  traceId?: string
  code?: string
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const responseTraceId =
      error.response?.data?.traceId ??
      (error.response?.headers?.['x-trace-id'] as string | undefined)

    const requestError: RequestError = {
      message: error.response?.data?.message ?? '服务暂不可用，请稍后重试',
      traceId: responseTraceId,
      code: error.response?.data?.code,
    }

    return Promise.reject(requestError)
  },
)

export default request


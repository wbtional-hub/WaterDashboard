import { defineStore } from 'pinia'

export const useSystemStore = defineStore('system', {
  state: () => ({
    applicationName: '统一智慧水务大屏系统',
    currentStage: '第一阶段最小闭环',
  }),
})


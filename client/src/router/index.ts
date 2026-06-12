import { createRouter, createWebHistory } from 'vue-router'

import HealthView from '@/pages/HealthView.vue'
import HomeView from '@/pages/HomeView.vue'
import DataSourcesView from '@/pages/DataSourcesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/health',
      name: 'health',
      component: HealthView,
    },
    {
      path: '/data-sources',
      name: 'data-sources',
      component: DataSourcesView,
    },
  ],
})

export default router

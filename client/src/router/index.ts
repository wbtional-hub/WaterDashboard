import { createRouter, createWebHistory } from 'vue-router'

import HealthView from '@/pages/HealthView.vue'
import HomeView from '@/pages/HomeView.vue'

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
  ],
})

export default router


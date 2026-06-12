import { createRouter, createWebHistory } from 'vue-router'

import HealthView from '@/pages/HealthView.vue'
import HomeView from '@/pages/HomeView.vue'
import DataSourcesView from '@/pages/DataSourcesView.vue'
import ComponentTemplatesView from '@/pages/ComponentTemplatesView.vue'
import DashboardsView from '@/pages/DashboardsView.vue'
import DashboardEditorView from '@/pages/DashboardEditorView.vue'

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
    {
      path: '/component-templates',
      name: 'component-templates',
      component: ComponentTemplatesView,
    },
    {
      path: '/dashboards',
      name: 'dashboards',
      component: DashboardsView,
    },
    {
      path: '/dashboards/:id/editor',
      name: 'dashboard-editor',
      component: DashboardEditorView,
    },
  ],
})

export default router

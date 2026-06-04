<script setup lang="ts">
import { useSystemStore } from '@/stores/system'

const systemStore = useSystemStore()
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div>
        <p class="eyebrow">WATER DASHBOARD PLATFORM</p>
        <h1>{{ systemStore.applicationName }}</h1>
      </div>
      <nav aria-label="主导航">
        <RouterLink to="/">系统首页</RouterLink>
        <RouterLink to="/health">健康检查</RouterLink>
      </nav>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <span>独立微服务产品</span>
      <span>{{ systemStore.currentStage }}</span>
    </footer>
  </div>
</template>


<script setup lang="ts">
import { useSystemStore } from '@/stores/system'

const systemStore = useSystemStore()
</script>

<template>
  <section class="intro-section">
    <div class="intro-copy">
      <p class="section-label">工程骨架已就绪</p>
      <h2>{{ systemStore.applicationName }}</h2>
      <p class="intro-text">
        独立部署、独立启动、独立运行的智慧水务大屏微服务产品。
      </p>
    </div>

    <dl class="status-list">
      <div>
        <dt>当前阶段</dt>
        <dd>{{ systemStore.currentStage }}</dd>
      </div>
      <div>
        <dt>前端状态</dt>
        <dd><span class="status-dot"></span>运行中</dd>
      </div>
      <div>
        <dt>下一项检查</dt>
        <dd><RouterLink to="/health">查看后端健康状态</RouterLink></dd>
      </div>
    </dl>
  </section>
</template>


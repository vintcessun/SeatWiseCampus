<template>
  <el-tooltip :content="$t('common.switchLang')" placement="bottom">
    <el-button
      circle
      :aria-label="$t('common.switchLang')"
      class="lang-switch"
      @click="onToggle"
    >{{ locale === 'zh' ? '中' : 'EN' }}</el-button>
  </el-tooltip>
</template>

<script setup>
import { locale, toggleLocale } from '../i18n'
function onToggle() { toggleLocale() }
</script>

<style scoped>
.lang-switch { font-weight: 600; font-size: 13px; }
</style>

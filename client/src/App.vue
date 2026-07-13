<template>
  <el-config-provider :locale="elLocale">
    <a href="#main-content" class="skip-link">{{ $t('common.skipToContent') }}</a>
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { locale } from './i18n'

const elLocale = computed(() => (locale.value === 'zh' ? zhCn : en))
</script>

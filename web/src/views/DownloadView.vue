<script setup lang="ts">
import { ref, onMounted } from 'vue'

const apiUrl = import.meta.env.VITE_API_URL || 'https://braza.lurdisson.com.br/api'
const baseUrl = apiUrl.replace('/api', '')
const apkUrl = `${baseUrl}/downloads/app-release.apk`

const version = ref('1.0.16')
const isLoading = ref(true)

onMounted(async () => {
  // Try to fetch version info
  try {
    const response = await fetch(`${apiUrl}/version`)
    if (response.ok) {
      const data = await response.json()
      if (data.version) {
        version.value = data.version
      }
    }
  } catch (e) {
    // Ignore errors, use default version
  }
  isLoading.value = false
})
</script>

<template>
  <div class="min-h-screen bg-dark-900 flex items-center justify-center p-4">
    <div class="max-w-md w-full">
      <!-- Card -->
      <div class="bg-dark-800 rounded-2xl shadow-xl overflow-hidden">
        <!-- Header with logo -->
        <div class="bg-gradient-to-br from-primary-600 to-primary-700 p-8 text-center">
          <img src="/braza_logo.png" alt="Made in Braza" class="w-24 h-24 mx-auto mb-4" />
          <h1 class="text-2xl font-bold text-white">Made in Braza</h1>
          <p class="text-primary-200 mt-1">App para Android</p>
        </div>

        <!-- Content -->
        <div class="p-6 space-y-6">
          <!-- Version info -->
          <div class="text-center">
            <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-primary-500/20 text-primary-400">
              <span v-if="isLoading" class="animate-pulse">Carregando...</span>
              <span v-else>Versao {{ version }}</span>
            </span>
          </div>

          <!-- Features -->
          <div class="space-y-3">
            <div class="flex items-center gap-3 text-gray-300">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>Chat em tempo real com o cla</span>
            </div>
            <div class="flex items-center gap-3 text-gray-300">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>Gerenciamento de Siege War</span>
            </div>
            <div class="flex items-center gap-3 text-gray-300">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>Criacao de parties por classes</span>
            </div>
            <div class="flex items-center gap-3 text-gray-300">
              <svg class="w-5 h-5 text-green-500 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>Notificacoes push</span>
            </div>
          </div>

          <!-- Download button -->
          <a
            :href="apkUrl"
            download
            class="flex items-center justify-center gap-2 w-full py-3 px-4 bg-primary-500 hover:bg-primary-600 text-white font-semibold rounded-xl transition-colors"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Baixar APK
          </a>

          <!-- Installation instructions -->
          <div class="text-sm text-gray-400 space-y-2">
            <p class="font-medium text-gray-300">Como instalar:</p>
            <ol class="list-decimal list-inside space-y-1">
              <li>Baixe o arquivo APK</li>
              <li>Abra o arquivo no seu Android</li>
              <li>Permita a instalacao de fontes desconhecidas</li>
              <li>Instale e aproveite!</li>
            </ol>
          </div>
        </div>

        <!-- Footer -->
        <div class="bg-dark-700 px-6 py-4 text-center">
          <a href="/web/login" class="text-primary-400 hover:text-primary-300 text-sm">
            Ja tem conta? Acessar pelo navegador
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

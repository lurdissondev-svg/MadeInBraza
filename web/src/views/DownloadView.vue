<script setup lang="ts">
import { ref, onMounted } from 'vue'

const GITHUB_REPO = 'lurdissondev-svg/MadeInBraza'
const releasesUrl = `https://github.com/${GITHUB_REPO}/releases`

const version = ref('')
const apkUrl = ref('')
const isLoading = ref(true)
const error = ref(false)

onMounted(async () => {
  try {
    // Fetch latest release from GitHub API
    const response = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`)
    if (response.ok) {
      const data = await response.json()
      version.value = data.tag_name || data.name || 'Latest'

      // Find APK asset
      const apkAsset = data.assets?.find((asset: { name: string }) =>
        asset.name.endsWith('.apk')
      )
      if (apkAsset) {
        apkUrl.value = apkAsset.browser_download_url
      }
    } else {
      error.value = true
    }
  } catch (e) {
    error.value = true
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
              <span v-else-if="version">Versao {{ version }}</span>
              <span v-else>Ultima versao</span>
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
          <div v-if="isLoading" class="flex items-center justify-center py-3">
            <svg class="animate-spin h-6 w-6 text-primary-500" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
          <a
            v-else-if="apkUrl"
            :href="apkUrl"
            class="flex items-center justify-center gap-2 w-full py-3 px-4 bg-primary-500 hover:bg-primary-600 text-white font-semibold rounded-xl transition-colors"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Baixar APK
          </a>
          <a
            v-else
            :href="releasesUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center justify-center gap-2 w-full py-3 px-4 bg-primary-500 hover:bg-primary-600 text-white font-semibold rounded-xl transition-colors"
          >
            <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path fill-rule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clip-rule="evenodd" />
            </svg>
            Ver no GitHub
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

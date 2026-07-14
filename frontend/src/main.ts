import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'
import './styles/main.css'

const app = createApp(App); const pinia = createPinia(); app.use(pinia).use(router).use(ElementPlus)
setUnauthorizedHandler(() => { useAuthStore(pinia).clear(); if (router.currentRoute.value.name !== 'login') router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } }) })
const auth = useAuthStore(pinia); if (auth.token) auth.fetchMe().catch(() => auth.clear())
app.mount('#app')

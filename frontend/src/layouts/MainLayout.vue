<script setup lang="ts">
import { House, Shop, Document, User, Ticket, Plus, SwitchButton, Connection } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const router = useRouter(); const auth = useAuthStore()
function logout() { auth.clear(); router.push('/login') }
</script>

<template>
  <el-container class="shell">
    <el-aside width="220px" class="sidebar">
      <div class="brand"><span>HM</span><div>HM Dianping<small>API workspace</small></div></div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/"><el-icon><House /></el-icon><span>Home</span></el-menu-item>
        <el-menu-item index="/shops"><el-icon><Shop /></el-icon><span>Shops</span></el-menu-item>
        <el-menu-item index="/blogs"><el-icon><Document /></el-icon><span>Blogs</span></el-menu-item>
        <el-menu-item index="/feed"><el-icon><Connection /></el-icon><span>Following</span></el-menu-item>
        <el-menu-item index="/vouchers"><el-icon><Ticket /></el-icon><span>Vouchers</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon><span>Profile</span></el-menu-item>
      </el-menu>
      <div class="sidebar-bottom"><el-button plain :icon="Plus" @click="router.push('/blogs/new')">New blog</el-button></div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div><b>{{ $route.meta.title }}</b><span> · Spring Boot API workspace</span></div>
        <div v-if="auth.isLoggedIn" class="user-bar"><el-avatar :src="auth.user?.icon" :size="30">{{ auth.user?.nickName?.slice(0, 1) }}</el-avatar><span data-testid="user-name">{{ auth.user?.nickName || 'Signed in' }}</span><el-button link :icon="SwitchButton" @click="logout">Sign out</el-button></div>
        <el-button v-else type="primary" size="small" @click="router.push('/login')">Sign in</el-button>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

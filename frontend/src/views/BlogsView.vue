<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Blog } from '@/types/domain'
import BlogCard from '@/components/BlogCard.vue'
const router = useRouter(); const blogs = ref<Blog[]>([]); const current = ref(1); const loading = ref(false)
async function load() { loading.value = true; try { blogs.value = await blogApi.hot(current.value) } finally { loading.value = false } }
onMounted(load)
</script>
<template><div class="page-head"><div><h2>热门笔记</h2><p class="muted">按点赞数倒序；登录后可进入详情、点赞和查看点赞用户。</p></div><div><el-button @click="router.push('/blogs/mine')">我的笔记</el-button><el-button type="primary" @click="router.push('/blogs/new')">发布笔记</el-button></div></div><el-skeleton :loading="loading" animated :count="6"><div class="grid grid-3"><BlogCard v-for="blog in blogs" :key="blog.id" :blog="blog" @open="router.push(`/blogs/${$event}`)" /></div></el-skeleton><div class="pager"><el-button :disabled="current===1" @click="current--;load()">上一页</el-button><span>第 {{current}} 页</span><el-button :disabled="blogs.length < 10" @click="current++;load()">下一页</el-button></div></template>
<style scoped>.pager{display:flex;justify-content:center;gap:14px;align-items:center;margin-top:20px;font-size:13px;color:#64748b}</style>

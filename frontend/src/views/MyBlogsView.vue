<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { blogApi } from '@/api/blog'
import type { Blog } from '@/types/domain'
import BlogCard from '@/components/BlogCard.vue'
const router = useRouter(); const blogs = ref<Blog[]>([]); const current = ref(1); async function load(){blogs.value=await blogApi.mine(current.value)} onMounted(load)
</script>
<template><div class="page-head"><div><h2>我的笔记</h2><p class="muted">该接口直接返回数据库字段，不补充作者和当前点赞状态。</p></div><el-button type="primary" @click="router.push('/blogs/new')">发布笔记</el-button></div><el-empty v-if="!blogs.length" description="还没有发布笔记" /><div v-else class="grid grid-3"><BlogCard v-for="blog in blogs" :key="blog.id" :blog="blog" @open="router.push(`/blogs/${$event}`)" /></div><div class="pager"><el-button :disabled="current===1" @click="current--;load()">上一页</el-button><span>第 {{current}} 页</span><el-button :disabled="blogs.length<10" @click="current++;load()">下一页</el-button></div></template>
<style scoped>.pager{display:flex;justify-content:center;gap:14px;align-items:center;margin-top:20px;font-size:13px;color:#64748b}</style>

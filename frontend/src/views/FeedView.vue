<script setup lang="ts">
import { ref } from 'vue'
import { blogApi } from '@/api/blog'
import type { Blog } from '@/types/domain'
import BlogCard from '@/components/BlogCard.vue'
import { useRouter } from 'vue-router'
const router=useRouter(); const blogs=ref<Blog[]>([]); const lastId=ref(Date.now()); const offset=ref(0); const loading=ref(false); const ended=ref(false)
async function load(){if(loading.value||ended.value)return;loading.value=true;try{const result=await blogApi.feed(lastId.value,offset.value);if(!result){ended.value=true;return} blogs.value.push(...result.list);lastId.value=result.minTime;offset.value=result.offset;if(!result.list.length)ended.value=true}finally{loading.value=false}}
</script>
<template><div class="page-head"><div><h2>关注动态</h2><p class="muted">使用 Redis ZSet 游标分页，每次加载最多两条。</p></div><el-button type="primary" :loading="loading" @click="load">{{blogs.length?'加载更多':'加载动态'}}</el-button></div><el-empty v-if="!blogs.length&&!loading" description="点击“加载动态”开始；没有关注内容会返回空结果。" /><div v-else class="grid grid-3"><BlogCard v-for="blog in blogs" :key="blog.id" :blog="blog" @open="router.push(`/blogs/${$event}`)" /></div><p v-if="ended&&blogs.length" class="end">已经到底了</p></template>
<style scoped>.end{text-align:center;color:#94a3b8;margin:22px}</style>

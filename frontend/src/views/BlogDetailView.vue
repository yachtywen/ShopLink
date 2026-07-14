<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { blogApi } from '@/api/blog'
import type { Blog, UserDTO } from '@/types/domain'
import { splitImages, toImageUrl } from '@/utils/image'
const route = useRoute(); const router = useRouter(); const id = Number(route.params.id); const blog = ref<Blog>(); const likes = ref<UserDTO[]>([]); const liking = ref(false)
async function load() { [blog.value, likes.value] = await Promise.all([blogApi.get(id), blogApi.likes(id)]) }
async function toggleLike() { liking.value = true; try { await blogApi.like(id); await load(); ElMessage.success(blog.value?.isLike ? '已点赞' : '已取消点赞') } finally { liking.value = false } }
onMounted(load)
</script>
<template><div v-if="blog" class="detail"><div class="page-head"><div><h2>{{ blog.title }}</h2><p class="muted">{{ blog.createTime }} · 作者 <el-link @click="router.push(`/users/${blog.userId}`)">{{ blog.name || blog.userId }}</el-link></p></div><el-button :type="blog.isLike ? 'danger' : 'primary'" :loading="liking" @click="toggleLike">{{ blog.isLike ? '取消点赞' : '点赞' }} {{ blog.liked || 0 }}</el-button></div><el-card shadow="never"><div class="images"><el-image v-for="path in splitImages(blog.images)" :key="path" :src="toImageUrl(path)" fit="cover" :preview-src-list="splitImages(blog.images).map(toImageUrl)" /><div v-if="!splitImages(blog.images).length" class="image-placeholder no-image">暂无图片</div></div><p class="content">{{ blog.content || '暂无正文' }}</p></el-card><el-card shadow="never" class="likes"><template #header>最近点赞用户（最多 5 位）</template><el-empty v-if="!likes.length" description="暂无点赞" /><el-space v-else wrap><el-tag v-for="user in likes" :key="user.id" @click="router.push(`/users/${user.id}`)">{{ user.nickName }}</el-tag></el-space></el-card></div><el-skeleton v-else :rows="8" animated /></template>
<style scoped>.detail{max-width:900px}.images{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}.images .el-image,.no-image{height:200px}.content{white-space:pre-wrap;font-size:15px;line-height:1.8;margin:22px 0 6px}.likes{margin-top:16px}</style>

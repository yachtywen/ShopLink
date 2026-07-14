<script setup lang="ts">
import { ChatDotRound, Star } from '@element-plus/icons-vue'
import type { Blog } from '@/types/domain'
import { splitImages, toImageUrl } from '@/utils/image'
defineProps<{ blog: Blog }>()
defineEmits<{ open: [id: number] }>()
</script>
<template>
  <el-card class="card-hover blog-card" shadow="never" data-testid="blog-card" @click="$emit('open', blog.id!)">
    <el-image v-if="splitImages(blog.images)[0]" :src="toImageUrl(splitImages(blog.images)[0])" fit="cover" class="cover"><template #error><div class="image-placeholder">图片不可用</div></template></el-image>
    <div class="blog-content"><b>{{ blog.title || '无标题笔记' }}</b><p>{{ blog.content || '暂无正文内容' }}</p><div class="blog-meta"><span>{{ blog.name || '用户 ' + (blog.userId || '-') }}</span><span><el-icon><Star /></el-icon>{{ blog.liked || 0 }}</span><span><el-icon><ChatDotRound /></el-icon>{{ blog.comments || 0 }}</span></div></div>
  </el-card>
</template>
<style scoped>.blog-card{padding:0}.blog-card :deep(.el-card__body){padding:0}.cover{width:100%;height:150px;background:#e2e8f0}.blog-content{padding:13px}.blog-content p{font-size:13px;color:#64748b;height:36px;overflow:hidden;margin:8px 0}.blog-meta{display:flex;gap:12px;color:#94a3b8;font-size:12px;align-items:center}.blog-meta span:first-child{margin-right:auto}.blog-meta .el-icon{vertical-align:-2px;margin-right:2px}</style>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { blogApi } from '@/api/blog'
import { joinImages } from '@/utils/image'
import ImageUploader from '@/components/ImageUploader.vue'
const router = useRouter(); const form = reactive({ shopId: undefined as number | undefined, title: '', content: '' }); const images = ref<string[]>([]); const saving = ref(false)
async function save() { if (!form.shopId || !form.title || !form.content) return ElMessage.warning('请填写店铺 ID、标题和正文'); saving.value = true; try { const id = await blogApi.create({ ...form, images: joinImages(images.value) }); ElMessage.success('笔记发布成功'); router.push(`/blogs/${id}`) } finally { saving.value = false } }
</script>
<template><div class="page-head"><div><h2>发布探店笔记</h2><p class="muted">作者由后端根据当前 Token 自动写入。</p></div></div><el-card shadow="never" class="editor"><el-form :model="form" label-width="80px"><el-form-item label="店铺 ID" required><el-input-number v-model="form.shopId" :min="1" /></el-form-item><el-form-item label="标题" required><el-input v-model="form.title" maxlength="64" show-word-limit /></el-form-item><el-form-item label="图片"><ImageUploader v-model="images" /></el-form-item><el-form-item label="正文" required><el-input v-model="form.content" type="textarea" :rows="8" maxlength="2000" show-word-limit /></el-form-item><el-form-item><el-button type="primary" :loading="saving" @click="save">发布笔记</el-button><el-button @click="router.back()">取消</el-button></el-form-item></el-form></el-card></template>
<style scoped>.editor{max-width:860px}</style>

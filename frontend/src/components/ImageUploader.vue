<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Plus } from '@element-plus/icons-vue'
import { blogApi } from '@/api/blog'
import { toImageUrl } from '@/utils/image'
const model = defineModel<string[]>({ required: true })
const uploading = ref(false)
async function upload(file: File) { uploading.value = true; try { const path = await blogApi.upload(file); model.value = [...model.value, path]; ElMessage.success('图片已上传') } finally { uploading.value = false } }
async function remove(path: string) { await blogApi.removeImage(path); model.value = model.value.filter((item) => item !== path) }
function beforeUpload(file: File) { if (!file.type.startsWith('image/')) { ElMessage.error('请选择图片文件'); return false } if (file.size > 5 * 1024 * 1024) { ElMessage.error('单张图片不能超过 5MB'); return false } void upload(file); return false }
</script>
<template><el-upload list-type="picture-card" :show-file-list="false" :disabled="uploading || model.length >= 9" :before-upload="beforeUpload"><el-icon><Plus /></el-icon></el-upload><div class="upload-list"><div v-for="path in model" :key="path" class="thumb"><img :src="toImageUrl(path)" /><el-button circle type="danger" size="small" @click="remove(path)"><el-icon><Close /></el-icon></el-button></div></div><p class="muted">最多 9 张；后端当前未限制文件类型与大小，前端按图片且 5MB 限制。</p></template>
<style scoped>.upload-list{display:flex;gap:10px;flex-wrap:wrap;margin-top:10px}.thumb{width:100px;height:100px;position:relative}.thumb img{width:100%;height:100%;object-fit:cover;border-radius:6px}.thumb .el-button{position:absolute;right:-7px;top:-7px}</style>

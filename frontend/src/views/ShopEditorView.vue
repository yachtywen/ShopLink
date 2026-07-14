<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import type { Shop, ShopType } from '@/types/domain'
const route = useRoute(); const router = useRouter(); const id = route.params.id ? Number(route.params.id) : undefined; const types = ref<ShopType[]>([]); const saving = ref(false); const form = reactive<Shop>({ name:'', typeId: undefined, images:'', area:'', address:'', x:undefined, y:undefined, avgPrice:undefined, openHours:'' })
onMounted(async () => { types.value = await shopApi.types(); if (id) Object.assign(form, await shopApi.get(id)) })
async function save() { saving.value = true; try { if (id) await shopApi.update({ ...form, id }); else await shopApi.create(form); ElMessage.success('店铺已保存'); router.push('/shops') } finally { saving.value = false } }
</script>
<template><div class="page-head"><div><h2>{{ id ? '编辑店铺' : '新增店铺' }}</h2><p class="muted">此功能仅供后端 API 联调使用，后端当前只校验登录态。</p></div></div><el-card shadow="never" class="editor-card"><el-form :model="form" label-width="90px"><el-form-item label="店铺名称" required><el-input v-model="form.name" /></el-form-item><el-form-item label="店铺分类" required><el-select v-model="form.typeId" style="width:100%"><el-option v-for="type in types" :key="type.id" :value="type.id" :label="type.name" /></el-select></el-form-item><el-form-item label="图片路径"><el-input v-model="form.images" placeholder="逗号分隔的 /blogs/... 图片路径" /></el-form-item><el-form-item label="商圈"><el-input v-model="form.area" /></el-form-item><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item><el-form-item label="经纬度"><el-input-number v-model="form.x" :precision="6" /><span class="coords">经度</span><el-input-number v-model="form.y" :precision="6" /><span class="coords">纬度</span></el-form-item><el-form-item label="人均"><el-input-number v-model="form.avgPrice" :min="0" /></el-form-item><el-form-item label="营业时间"><el-input v-model="form.openHours" placeholder="10:00-22:00" /></el-form-item><el-form-item><el-button type="primary" :loading="saving" @click="save">保存</el-button><el-button @click="router.back()">取消</el-button></el-form-item></el-form></el-card></template>
<style scoped>.editor-card{max-width:760px}.coords{margin:0 12px 0 5px;color:#64748b;font-size:13px}</style>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { voucherApi } from '@/api/voucher'
import { useOrderPolling } from '@/composables/useOrderPolling'
const route=useRoute();const router=useRouter();const id=Number(route.params.id);const {order,polling,start,refresh,stop}=useOrderPolling();const terminal=[13,14,15]
async function pay(success:boolean){const text=await voucherApi.mockPay(id,success);ElMessage.success(text);await refresh(id)}
onMounted(()=>void start(id))
</script>
<template><div class="page-head"><div><h2>秒杀订单状态</h2><p class="muted">订单 {{id}} · {{polling?'每秒自动轮询中':'轮询已停止'}}</p></div><el-button @click="router.push('/vouchers')">返回优惠券</el-button></div><el-card shadow="never" class="order"><el-skeleton :loading="!order" animated><template #default><el-result data-testid="order-status" :data-status-code="String(order?.statusCode || '')" :icon="order?.statusCode===13?'success':order?.statusCode===12?'warning':'info'" :title="order?.statusText" :sub-title="`状态码 ${order?.statusCode} · 创建于 ${order?.createTime||'-'}`"><template #extra><el-space><el-button v-if="order?.statusCode===12" type="primary" @click="pay(true)">模拟支付成功</el-button><el-button v-if="order?.statusCode===12" @click="pay(false)">模拟支付失败</el-button><el-button v-if="terminal.includes(order?.statusCode||0)" @click="start(id)">重新查询</el-button><el-button v-if="polling" @click="stop">停止轮询</el-button></el-space></template></el-result><el-alert v-if="order?.statusCode===12" type="warning" :closable="false" title="这是开发演示用的 mock-pay 接口，不是真实支付。" /></template></el-skeleton></el-card></template>
<style scoped>.order{max-width:760px;margin:auto}</style>

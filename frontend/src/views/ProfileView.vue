<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import type { UserInfo } from '@/types/domain'
const auth=useAuthStore();const info=ref<UserInfo|null>(null);const count=ref(0);const signing=ref(false)
onMounted(async()=>{if(!auth.user)await auth.fetchMe();if(auth.user){[info.value,count.value]=await Promise.all([userApi.getInfo(auth.user.id),userApi.signCount()])}})
async function sign(){signing.value=true;try{await userApi.sign();count.value=await userApi.signCount();ElMessage.success('今日签到成功')}finally{signing.value=false}}
</script>
<template><div class="page-head"><div><h2>个人中心</h2><p class="muted">当前后端未实现资料编辑与真实登出。</p></div><el-button type="primary" :loading="signing" @click="sign">今日签到</el-button></div><div class="grid grid-2"><el-card shadow="never"><div class="profile-top"><el-avatar :size="72" :src="auth.user?.icon">{{auth.user?.nickName?.slice(0,1)}}</el-avatar><div><h3>{{auth.user?.nickName}}</h3><p>ID: {{auth.user?.id}}</p></div></div><el-descriptions :column="1" border><el-descriptions-item label="城市">{{info?.city||'-'}}</el-descriptions-item><el-descriptions-item label="简介">{{info?.introduce||'-'}}</el-descriptions-item><el-descriptions-item label="性别">{{info?.gender === undefined ? '-' : info.gender ? '男' : '女'}}</el-descriptions-item><el-descriptions-item label="积分">{{info?.credits??'-'}}</el-descriptions-item></el-descriptions></el-card><el-card shadow="never"><template #header>签到状态</template><div class="sign-card"><b>{{count}}</b><span>连续签到天数</span></div><el-alert type="info" :closable="false" title="签到由 Redis Bitmap 记录；如果今天未签到，连续天数会显示为 0。" /></el-card></div></template>
<style scoped>.profile-top{display:flex;align-items:center;gap:16px;margin-bottom:20px}.profile-top h3{margin:0}.profile-top p{color:#64748b;font-size:13px}.sign-card{padding:35px;text-align:center}.sign-card b{font-size:52px;color:#f97316;display:block}.sign-card span{color:#64748b}</style>

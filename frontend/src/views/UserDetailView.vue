<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { followApi } from '@/api/follow'
import { blogApi } from '@/api/blog'
import type { Blog, UserDTO, UserInfo } from '@/types/domain'
import BlogCard from '@/components/BlogCard.vue'
const route=useRoute();const router=useRouter();const id=Number(route.params.id);const user=ref<UserDTO|null>(null);const info=ref<UserInfo|null>(null);const followed=ref(false);const commons=ref<UserDTO[]>([]);const blogs=ref<Blog[]>([]);const changing=ref(false)
async function load(){[user.value,info.value,followed.value,commons.value,blogs.value]=await Promise.all([userApi.getById(id),userApi.getInfo(id),followApi.isFollowing(id),followApi.commons(id),blogApi.ofUser(id)])}
async function toggle(){changing.value=true;try{await followApi.change(id,!followed.value);followed.value=!followed.value;ElMessage.success(followed.value?'已关注':'已取消关注')}finally{changing.value=false}}
onMounted(load)
</script>
<template><div class="page-head"><div><h2>{{user?.nickName||'用户资料'}}</h2><p class="muted">用户 ID: {{id}} · {{info?.city||'未填写城市'}}</p></div><el-button type="primary" :loading="changing" @click="toggle">{{followed?'取消关注':'关注用户'}}</el-button></div><el-card shadow="never"><el-descriptions :column="3" border><el-descriptions-item label="简介">{{info?.introduce||'-'}}</el-descriptions-item><el-descriptions-item label="粉丝数">{{info?.fans??'-'}}</el-descriptions-item><el-descriptions-item label="关注数">{{info?.followee??'-'}}</el-descriptions-item></el-descriptions></el-card><el-card shadow="never" class="commons"><template #header>共同关注</template><el-empty v-if="!commons.length" description="暂无共同关注" /><el-space v-else wrap><el-tag v-for="item in commons" :key="item.id" @click="router.push(`/users/${item.id}`)">{{item.nickName}}</el-tag></el-space></el-card><div class="page-head compact"><h2>TA 的笔记</h2></div><div class="grid grid-3"><BlogCard v-for="blog in blogs" :key="blog.id" :blog="blog" @open="router.push(`/blogs/${$event}`)" /></div></template>
<style scoped>.commons{margin-top:16px}.compact{margin:22px 0 14px}</style>

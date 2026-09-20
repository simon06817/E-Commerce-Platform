<template>
  <div class="admin-profile">
    <header class="page-heading">
      <div>
        <h1>账号设置</h1>
        <p>维护管理员联系方式并修改登录密码。</p>
      </div>
    </header>

    <section class="profile-panel surface">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="账号资料" name="profile">
          <el-form label-position="top" class="profile-form">
            <el-form-item label="登录账号">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model.trim="form.phone" maxlength="20" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model.trim="form.email" maxlength="100" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">
              保存资料
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form label-position="top" class="profile-form">
            <el-form-item label="原密码">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-button
              type="primary"
              :loading="changingPassword"
              @click="changePassword"
            >
              修改密码
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { profileApi } from '../api'

const activeTab = ref('profile')
const saving = ref(false)
const changingPassword = ref(false)
const profile = reactive({
  username: '',
  phone: '',
  email: ''
})
const form = reactive({
  phone: '',
  email: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function loadProfile() {
  try {
    Object.assign(profile, await profileApi.get())
    Object.assign(form, {
      phone: profile.phone || '',
      email: profile.email || ''
    })
  } catch (error) {
    ElMessage.error(error.message || '账号资料加载失败')
  }
}

async function saveProfile() {
  saving.value = true
  try {
    Object.assign(profile, await profileApi.update(form))
    ElMessage.success('账号资料已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await profileApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    Object.assign(passwordForm, {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    ElMessage.success('密码已修改')
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.admin-profile {
  display: grid;
  gap: 18px;
}

.page-heading h1,
.page-heading p {
  margin: 0;
}

.page-heading h1 {
  font-size: 28px;
}

.page-heading p {
  margin-top: 5px;
  color: var(--muted);
}

.profile-panel {
  padding: 20px;
}

.profile-form {
  width: min(560px, 100%);
}
</style>

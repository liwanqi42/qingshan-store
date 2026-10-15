<template>
  <div class="force-password-page">
    <el-card class="force-password-card" shadow="always">
      <div slot="header" class="card-header">
        修改初始密码
      </div>
      <p class="tip">
        首次登录或管理员重置密码后，请先设置新密码，修改成功后需要重新登录。
      </p>
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="90px"
        @submit.native.prevent
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入初始密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="6-20 位新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
      </el-form>
      <div class="actions">
        <el-button @click="handleLogout">退出登录</el-button>
        <el-button type="primary" :loading="loading" @click="submit">
          确认修改
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import {editPassword} from '@/api/employee'
import {isValidPassword} from '@/utils/validate'

export default {
  name: 'ForcePasswordView',
  data() {
    const validateNewPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入新密码'))
        return
      }
      if (!isValidPassword(value)) {
        callback(new Error('密码长度为 6-20 位'))
        return
      }
      if (value === this.form.oldPassword) {
        callback(new Error('新密码不能与原密码相同'))
        return
      }
      callback()
    }

    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
        return
      }
      if (value !== this.form.newPassword) {
        callback(new Error('两次输入的密码不一致'))
        return
      }
      callback()
    }

    return {
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [
          { required: true, message: '请输入初始密码', trigger: 'blur' }
        ],
        newPassword: [
          { validator: validateNewPassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    submit() {
      if (this.loading) {
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        editPassword({
          oldPassword: this.form.oldPassword,
          newPassword: this.form.newPassword
        })
          .then(() => {
            this.$message.success('密码修改成功，请重新登录')
            this.$store.dispatch('user/clearLoginState')
            this.$store.dispatch('tagsView/resetViews')
            this.$router.replace('/login')
          })
          .catch(() => {})
          .finally(() => {
            this.loading = false
          })
      })
    },
    handleLogout() {
      this.$store
        .dispatch('user/logout')
        .catch(() => {})
        .finally(() => {
          this.$router.replace('/login')
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.force-password-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.force-password-card {
  width: 460px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.tip {
  margin: 0 0 20px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

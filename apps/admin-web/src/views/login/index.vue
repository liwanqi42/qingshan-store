<template>
  <div class="login-container">
    <div class="login-brand">
      <div class="brand-logo">青</div>
      <h1>青山商店</h1>
      <p>让每一笔订单都清晰可控</p>
    </div>
    <div class="login-panel">
      <div class="login-header">
        <h2>欢迎登录</h2>
        <p>青山商店管理后台</p>
      </div>
      <el-form
        ref="loginForm"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter.native="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            clearable
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <div class="login-options">
          <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
        </div>
        <el-button
          type="primary"
          :loading="loading"
          class="login-button"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
      <p class="login-footer">© 2026 青山商店</p>
    </div>
  </div>
</template>

<script>
import { isValidUsername, isValidPassword } from '@/utils/validate'

const REMEMBER_KEY = 'admin-web-remember-username'

export default {
  name: 'LoginPage',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入用户名'))
        return
      }
      if (!isValidUsername(value)) {
        callback(new Error('用户名为 3-20 位字母、数字或下划线'))
        return
      }
      callback()
    }

    const validatePassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入密码'))
        return
      }
      if (!isValidPassword(value)) {
        callback(new Error('密码长度为 6-20 位'))
        return
      }
      callback()
    }

    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ validator: validateUsername, trigger: 'blur' }],
        password: [{ validator: validatePassword, trigger: 'blur' }]
      },
      rememberMe: false,
      loading: false
    }
  },
  mounted() {
    this.restoreRememberedUsername()

    if (window.location.search.includes('expired=1')) {
      this.$message.warning('登录状态已失效，请重新登录')
      window.history.replaceState(
        {},
        '',
        `${window.location.pathname}${window.location.hash}`
      )
    }
  },
  methods: {
    restoreRememberedUsername() {
      const username = localStorage.getItem(REMEMBER_KEY)
      if (username) {
        this.loginForm.username = username
        this.rememberMe = true
      }
    },
    persistRememberedUsername() {
      if (this.rememberMe) {
        localStorage.setItem(REMEMBER_KEY, this.loginForm.username)
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
    },
    handleLogin() {
      if (this.loading) {
        return
      }

      this.$refs.loginForm.validate(valid => {
        if (!valid) {
          return
        }

        this.persistRememberedUsername()
        this.loading = true

        this.$store
          .dispatch('user/login', {
            username: this.loginForm.username.trim(),
            password: this.loginForm.password
          })
          .then(() => {
            const redirect = this.$route.query.redirect
            const target =
              typeof redirect === 'string' &&
              redirect.startsWith('/') &&
              !redirect.startsWith('//')
                ? redirect
                : '/dashboard'
            this.$router.replace(target)
          })
          .catch(() => {})
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped src="../../styles/login.scss"></style>

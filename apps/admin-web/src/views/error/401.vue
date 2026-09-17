<template>
  <div class="error-page">
    <div class="error-card">
      <h1 class="error-code">401</h1>
      <p class="error-message">抱歉，你没有权限访问该页面</p>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goLogin">重新登录</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UnauthorizedPage',
  methods: {
    goHome() {
      this.$router.push('/')
    },
    goLogin() {
      this.$store.dispatch('user/clearLoginState')
      this.$router.push('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  background-color: $bg-page;
}

.error-card {
  text-align: center;
}

.error-code {
  margin: 0;
  font-size: 96px;
  color: $color-warning;
}

.error-message {
  margin: 16px 0 24px;
  color: $text-secondary;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>

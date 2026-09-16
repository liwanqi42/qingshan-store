<template>
  <div class="error-page">
    <div class="error-card">
      <h1 class="error-code">404</h1>
      <p class="error-message">抱歉，你访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  methods: {
    goHome() {
      this.$router.push('/')
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
  color: $color-primary;
}

.error-message {
  margin: 16px 0 24px;
  color: $text-secondary;
}
</style>

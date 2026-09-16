<template>
  <div class="login-container">
    <div class="login-card">
      <h2>青山商店管理后台</h2>
      <p>登录功能开发中</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginPage'
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  background-color: $bg-page;
}

.login-card {
  padding: 40px 60px;
  text-align: center;
  background-color: $bg-white;
  border-radius: $border-radius-base;
}

h2 {
  margin: 0 0 12px;
  color: $text-primary;
}

p {
  margin: 0;
  color: $text-secondary;
}
</style>

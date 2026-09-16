<template>
  <div class="hamburger-container" @click="handleToggle">
    <i :class="isActive ? 'el-icon-s-fold' : 'el-icon-s-unfold'" />
  </div>
</template>

<script>
export default {
  name: 'AppHamburger',
  props: {
    isActive: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    handleToggle() {
      this.$emit('toggle')
    }
  }
}
</script>

<style lang="scss" scoped>
.hamburger-container {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 50px;
  height: 100%;
  color: $text-regular;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.025);
  }

  i {
    font-size: 20px;
  }
}
</style>

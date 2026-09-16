<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="item.path"
    >
      <span
        v-if="item.redirect === 'noRedirect' || index === breadcrumbs.length - 1"
        class="no-redirect"
      >
        {{ item.meta.title }}
      </span>
      <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script>
export default {
  name: 'AppBreadcrumb',
  computed: {
    breadcrumbs() {
      let matched = this.$route.matched.filter(
        item => item.meta && item.meta.title && !item.meta.hidden
      )
      const first = matched[0]
      if (!first || first.name !== 'DashboardView') {
        matched = [
          { path: '/dashboard', meta: { title: '首页' } }
        ].concat(matched)
      }
      return matched
    }
  },
  methods: {
    handleLink(item) {
      const { redirect, path } = item
      if (redirect && redirect !== 'noRedirect') {
        this.$router.push(redirect)
        return
      }
      if (path) {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.app-breadcrumb {
  display: flex;
  align-items: center;
  height: 100%;
  font-size: 14px;

  ::v-deep .el-breadcrumb__inner {
    color: $text-secondary;
    font-weight: 400;
  }

  .no-redirect {
    color: $text-secondary;
    cursor: text;
  }
}
</style>

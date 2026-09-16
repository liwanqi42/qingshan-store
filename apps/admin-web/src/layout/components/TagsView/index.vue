<template>
  <div class="tags-view-container">
    <div class="tags-view-list">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path }"
        class="tags-view-item"
        :class="{ active: isActive(tag) }"
        @click.middle.native.prevent="closeSelectedTag(tag)"
      >
        <span class="tag-title">{{ tag.title }}</span>
        <i
          v-if="!tag.meta.affix"
          class="el-icon-close tag-close"
          @click.prevent.stop="closeSelectedTag(tag)"
        />
      </router-link>
    </div>
    <el-dropdown
      class="tags-view-actions"
      trigger="click"
      @command="handleCommand"
    >
      <span class="tags-view-action">
        <i class="el-icon-arrow-down" />
      </span>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="refresh">刷新当前</el-dropdown-item>
        <el-dropdown-item command="closeOthers">关闭其他</el-dropdown-item>
        <el-dropdown-item command="closeAll" divided>关闭全部</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'TagsView',
  computed: {
    ...mapState('tagsView', ['visitedViews'])
  },
  mounted() {
    this.initTags()
  },
  watch: {
    $route() {
      this.addCurrentView()
    }
  },
  methods: {
    initTags() {
      this.getAffixTags().forEach(tag => {
        this.$store.dispatch('tagsView/addView', tag)
      })
      this.addCurrentView()
    },
    getAffixTags() {
      const rootRoute = this.$router.options.routes.find(
        route => route.path === '/'
      )
      const children = rootRoute && rootRoute.children ? rootRoute.children : []
      return children
        .filter(route => route.meta && route.meta.affix)
        .map(route => ({
          path: this.getFullPath(route.path),
          name: route.name,
          meta: route.meta,
          title: route.meta.title
        }))
    },
    getFullPath(routePath) {
      return routePath.startsWith('/') ? routePath : `/${routePath}`
    },
    addCurrentView() {
      const route = this.$route
      if (!route.meta || !route.meta.title || route.meta.hidden) {
        return
      }
      this.$store.dispatch('tagsView/addView', {
        path: route.path,
        fullPath: route.fullPath,
        name: route.name,
        meta: route.meta,
        title: route.meta.title
      })
    },
    isActive(tag) {
      return tag.path === this.$route.path
    },
    closeSelectedTag(tag) {
      if (tag.meta && tag.meta.affix) {
        return
      }
      const remaining = this.visitedViews.filter(view => view.path !== tag.path)
      this.$store.dispatch('tagsView/delView', tag)
      if (this.isActive(tag)) {
        const latest = remaining[remaining.length - 1]
        this.$router.push(latest ? latest.path : '/dashboard')
      }
    },
    refreshSelectedTag() {
      this.$store.dispatch('tagsView/delCachedView', this.$route)
      const { fullPath } = this.$route
      this.$nextTick(() => {
        this.$router.replace(`/redirect${fullPath}`)
      })
    },
    closeOthersTags() {
      this.$store.dispatch('tagsView/delOthersViews', this.$route)
    },
    closeAllTags() {
      this.$store.dispatch('tagsView/delAllViews')
      if (!this.$route.meta.affix) {
        this.$router.push('/dashboard')
      }
    },
    handleCommand(command) {
      if (command === 'refresh') {
        this.refreshSelectedTag()
      } else if (command === 'closeOthers') {
        this.closeOthersTags()
      } else if (command === 'closeAll') {
        this.closeAllTags()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  height: $tags-view-height;
  padding-left: 8px;
  background-color: $bg-white;
  border-bottom: 1px solid $border-color-light;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  z-index: 9;
}

.tags-view-list {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  height: 24px;
  margin-right: 6px;
  padding: 0 8px;
  font-size: 12px;
  color: $text-regular;
  background-color: $bg-white;
  border: 1px solid $border-color;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: $color-primary;
    border-color: $color-primary;
  }

  &.active {
    color: #fff;
    background-color: $color-primary;
    border-color: $color-primary;

    .tag-close:hover {
      background-color: rgba(255, 255, 255, 0.25);
    }
  }
}

.tag-close {
  margin-left: 6px;
  font-size: 12px;
  border-radius: 50%;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.15);
  }
}

.tags-view-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 100%;
  color: $text-regular;
  border-left: 1px solid $border-color-light;
  cursor: pointer;

  &:hover {
    background-color: rgba(0, 0, 0, 0.025);
  }
}

.tags-view-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
</style>

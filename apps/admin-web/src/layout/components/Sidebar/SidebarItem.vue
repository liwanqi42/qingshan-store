<template>
  <div v-if="!item.meta || !item.meta.hidden">
    <template v-if="showSingleChild">
      <el-menu-item
        v-if="onlyOneChild.meta"
        :index="resolvePath(onlyOneChild.path)"
      >
        <i v-if="onlyOneChild.meta.icon" :class="onlyOneChild.meta.icon" />
        <span v-if="onlyOneChild.meta.title" slot="title">
          {{ onlyOneChild.meta.title }}
        </span>
      </el-menu-item>
    </template>
    <el-submenu v-else :index="basePath" popper-append-to-body>
      <template slot="title">
        <i v-if="item.meta && item.meta.icon" :class="item.meta.icon" />
        <span v-if="item.meta && item.meta.title">{{ item.meta.title }}</span>
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(child.path)"
        :is-first-level="false"
      />
    </el-submenu>
  </div>
</template>

<script>
import { isExternal } from '@/utils'

export default {
  name: 'SidebarItem',
  props: {
    item: {
      type: Object,
      required: true
    },
    isFirstLevel: {
      type: Boolean,
      default: true
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  computed: {
    showingChildren() {
      return (this.item.children || []).filter(
        child => !(child.meta && child.meta.hidden)
      )
    },
    onlyOneChild() {
      if (this.showingChildren.length === 1) {
        return this.showingChildren[0]
      }
      if (this.showingChildren.length === 0) {
        return { ...this.item, path: '' }
      }
      return null
    },
    showSingleChild() {
      return this.showingChildren.length <= 1
    }
  },
  methods: {
    resolvePath(routePath) {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(this.basePath)) {
        return this.basePath
      }
      if (!routePath) {
        return this.basePath
      }
      if (routePath.startsWith('/')) {
        return routePath
      }
      return `${this.basePath.replace(/\/$/, '')}/${routePath}`
    }
  }
}
</script>

<template>
  <div class="sidebar-container" :class="{ 'is-collapsed': collapse }">
    <div class="logo">
      <template v-if="!collapse">
        <span class="logo-title">青山商店</span>
        <span class="logo-subtitle">管理后台</span>
      </template>
      <span v-else class="logo-mark">青</span>
    </div>
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :background-color="menuBg"
        :text-color="menuText"
        :active-text-color="menuActiveText"
        :unique-opened="true"
        :collapse-transition="false"
        mode="vertical"
        router
      >
        <sidebar-item
          v-for="route in sidebarRoutes"
          :key="route.path"
          :item="route"
          :base-path="getBasePath(route.path)"
          :is-first-level="true"
        />
      </el-menu>
    </el-scrollbar>
    <more-manage :collapse="collapse" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import SidebarItem from './SidebarItem.vue'
import MoreManage from '../MoreManage/index.vue'
import { isExternal } from '@/utils'

export default {
  name: 'AppSidebar',
  components: {
    SidebarItem,
    MoreManage
  },
  props: {
    collapse: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      menuBg: '#304156',
      menuText: '#bfcbd9',
      menuActiveText: '#409eff'
    }
  },
  computed: {
    ...mapGetters('permission', ['sidebarRoutes']),
    activeMenu() {
      const { meta, path } = this.$route
      return (meta && meta.activeMenu) || path
    }
  },
  methods: {
    getBasePath(routePath) {
      if (isExternal(routePath)) {
        return routePath
      }
      return routePath.startsWith('/') ? routePath : `/${routePath}`
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-container {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: $sidebar-width;
  height: 100%;
  background-color: $sidebar-bg;
  transition: width $transition-base;
  overflow: hidden;

  &.is-collapsed {
    width: $sidebar-collapsed-width;
  }
}

.logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  height: $navbar-height;
  background-color: $sidebar-sub-bg;
  color: #fff;
}

.logo-title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
}

.logo-subtitle {
  margin-top: 2px;
  font-size: 10px;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.6);
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background-color: $color-primary;
  border-radius: $border-radius-base;
}

.el-scrollbar {
  flex: 1;
  min-height: 0;
}

::v-deep .scrollbar-wrapper {
  overflow-x: hidden !important;
}

::v-deep .el-menu {
  border-right: none;
}

::v-deep .el-menu--collapse {
  width: $sidebar-collapsed-width;
}

::v-deep .el-menu-item,
::v-deep .el-submenu__title {
  height: 50px;
  line-height: 50px;
}

::v-deep .el-menu-item:hover,
::v-deep .el-submenu__title:hover {
  background-color: $sidebar-hover-bg !important;
}

::v-deep .el-menu-item.is-active {
  background-color: $sidebar-sub-bg !important;
}
</style>

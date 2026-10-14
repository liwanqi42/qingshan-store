<template>
  <el-popover
    v-if="menus.length"
    v-model="visible"
    placement="top-start"
    trigger="click"
    append-to-body
    :width="collapse ? 180 : 210"
  >
    <div class="more-manage-list">
      <div
        v-for="item in menus"
        :key="item.name"
        class="more-manage-item"
        :class="{ 'is-active': isActive(item) }"
        @click="handleSelect(item)"
      >
        <i :class="item.meta.icon" class="more-manage-icon" />
        <span class="more-manage-title">{{ item.meta.title }}</span>
      </div>
    </div>

    <div
      slot="reference"
      class="more-manage-trigger"
      :class="{ 'is-collapsed': collapse }"
    >
      <i class="el-icon-more" />
      <span v-if="!collapse" class="more-manage-text">更多管理</span>
    </div>
  </el-popover>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'MoreManage',
  props: {
    collapse: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      visible: false
    }
  },
  computed: {
    ...mapGetters('permission', ['moreManageRoutes']),
    menus() {
      return this.moreManageRoutes
    }
  },
  methods: {
    resolvePath(item) {
      return item.path.startsWith('/') ? item.path : `/${item.path}`
    },
    isActive(item) {
      return this.$route.path === this.resolvePath(item)
    },
    handleSelect(item) {
      this.visible = false
      const path = this.resolvePath(item)
      if (this.$route.path !== path) {
        this.$router.push(path)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.more-manage-trigger {
  display: flex;
  align-items: center;
  height: 50px;
  padding: 0 20px;
  color: $sidebar-text;
  background-color: $sidebar-sub-bg;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    color: #fff;
    background-color: $sidebar-hover-bg;
  }

  &.is-collapsed {
    justify-content: center;
    padding: 0;
  }
}

.more-manage-trigger i {
  font-size: 18px;
}

.more-manage-text {
  margin-left: 10px;
  font-size: 14px;
}

.more-manage-list {
  display: flex;
  flex-direction: column;
}

.more-manage-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 10px;
  color: $text-regular;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: $transition-base;

  &:hover {
    color: $color-primary;
    background-color: rgba(64, 158, 255, 0.08);
  }

  &.is-active {
    color: $color-primary;
    background-color: rgba(64, 158, 255, 0.12);
  }
}

.more-manage-icon {
  margin-right: 8px;
  font-size: 16px;
}

.more-manage-title {
  font-size: 14px;
}
</style>

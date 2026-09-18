<template>
  <div class="navbar">
    <hamburger
      :is-active="!collapsed"
      class="hamburger-container"
      @toggle="toggleSidebar"
    />
    <breadcrumb class="breadcrumb-container" />
    <div class="right-menu">
      <el-dropdown
        class="user-dropdown-wrapper"
        trigger="click"
        @command="handleCommand"
      >
        <span class="user-dropdown">
          <i class="el-icon-user-solid user-avatar" />
          <span class="user-name">{{ name || '管理员' }}</span>
          <i class="el-icon-arrow-down" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="password" icon="el-icon-edit">
            修改密码
          </el-dropdown-item>
          <el-dropdown-item
            command="logout"
            icon="el-icon-switch-button"
            divided
          >
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <password-dialog :visible.sync="passwordDialogVisible" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Hamburger from '../Hamburger/index.vue'
import Breadcrumb from '../Breadcrumb/index.vue'
import PasswordDialog from '../PasswordDialog.vue'

export default {
  name: 'AppNavbar',
  components: {
    Hamburger,
    Breadcrumb,
    PasswordDialog
  },
  props: {
    collapsed: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      passwordDialogVisible: false
    }
  },
  computed: {
    ...mapGetters('user', ['name'])
  },
  methods: {
    toggleSidebar() {
      this.$emit('toggle-sidebar')
    },
    handleCommand(command) {
      if (command === 'password') {
        this.passwordDialogVisible = true
        return
      }
      if (command === 'logout') {
        this.logout()
      }
    },
    logout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store
            .dispatch('user/logout')
            .catch(() => {})
            .finally(() => {
              this.$router.push('/login')
            })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  height: $navbar-height;
  padding-right: 16px;
  background-color: $bg-white;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.hamburger-container {
  flex-shrink: 0;
}

.breadcrumb-container {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
}

.right-menu {
  display: flex;
  align-items: center;
  height: 100%;
}

.user-dropdown-wrapper {
  height: 100%;
}

.user-dropdown {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 10px;
  color: $text-regular;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.025);
  }
}

.user-avatar {
  font-size: 22px;
  color: $color-primary;
}

.user-name {
  margin: 0 6px 0 8px;
  font-size: 14px;
}

.el-icon-arrow-down {
  font-size: 12px;
  color: $text-secondary;
}
</style>

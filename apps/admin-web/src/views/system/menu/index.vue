<template>
  <div class="app-container">
    <el-alert
      title="当前为本地菜单配置，保存后立即生效；后端菜单接口接入后可替换为持久化接口。"
      type="info"
      :closable="false"
      show-icon
      class="mb-20"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <span class="toolbar-title">菜单配置</span>
        <el-button icon="el-icon-refresh-left" @click="handleReset">
          恢复默认
        </el-button>
      </div>

      <el-table
        :data="menuRows"
        row-key="key"
        border
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="title" label="菜单名称" min-width="140" />
        <el-table-column prop="path" label="路由" min-width="160" />
        <el-table-column label="图标" width="210">
          <template slot-scope="{ row }">
            <i :class="row.icon" class="menu-icon" />
            <span class="icon-name">{{ row.icon || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="显示" width="90">
          <template slot-scope="{ row }">
            <el-tag :type="row.hidden ? 'info' : 'success'" size="mini">
              {{ row.hidden ? '隐藏' : '显示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      title="编辑菜单"
      :visible.sync="editVisible"
      width="460px"
      append-to-body
    >
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="菜单名称">
          <el-input v-model="editForm.title" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select
            v-model="editForm.icon"
            placeholder="请选择图标"
            style="width: 100%"
          >
            <el-option
              v-for="icon in iconOptions"
              :key="icon"
              :label="icon"
              :value="icon"
            >
              <i :class="icon" class="menu-icon" />
              <span class="icon-name">{{ icon }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch v-model="editForm.visible" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { constantRoutes } from '@/router/routes'

const ICON_OPTIONS = [
  'el-icon-s-home',
  'el-icon-s-custom',
  'el-icon-s-grid',
  'el-icon-s-check',
  'el-icon-collection',
  'el-icon-s-order',
  'el-icon-s-goods',
  'el-icon-s-shop',
  'el-icon-s-data',
  'el-icon-setting'
]

export default {
  name: 'SystemMenuView',
  data() {
    return {
      editVisible: false,
      editForm: {
        key: '',
        title: '',
        icon: '',
        sort: 0,
        visible: true
      },
      iconOptions: ICON_OPTIONS
    }
  },
  computed: {
    ...mapState('menu', ['settings']),
    rootChildren() {
      const root = constantRoutes.find(route => route.path === '/')
      return root && root.children ? root.children : []
    },
    menuRows() {
      return this.buildRows(this.rootChildren)
    }
  },
  methods: {
    buildRows(routes, basePath = '') {
      return routes.map(route => {
        const key = route.name || route.path
        const config = this.settings[key] || {}
        const meta = route.meta || {}
        const fullPath = route.path.startsWith('/')
          ? route.path
          : `${basePath}/${route.path}`.replace(/\/+/g, '/')

        return {
          key,
          path: fullPath,
          title: config.title || meta.title || '',
          icon: config.icon || meta.icon || '',
          sort: config.sort === undefined ? 0 : config.sort,
          hidden: config.hidden === true,
          children: route.children
            ? this.buildRows(route.children, fullPath)
            : undefined
        }
      })
    },
    handleEdit(row) {
      this.editForm = {
        key: row.key,
        title: row.title,
        icon: row.icon,
        sort: row.sort,
        visible: !row.hidden
      }
      this.editVisible = true
    },
    saveEdit() {
      if (!this.editForm.title) {
        this.$message.warning('请输入菜单名称')
        return
      }

      const settings = {
        ...this.settings,
        [this.editForm.key]: {
          title: this.editForm.title,
          icon: this.editForm.icon,
          sort: Number(this.editForm.sort) || 0,
          hidden: !this.editForm.visible
        }
      }

      this.$store.dispatch('menu/saveSettings', settings)
      this.editVisible = false
      this.$message.success('菜单配置已保存')
    },
    handleReset() {
      this.$confirm('确定恢复默认菜单配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store.dispatch('menu/resetSettings')
          this.$message.success('已恢复默认菜单配置')
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.menu-icon {
  margin-right: 8px;
  font-size: 16px;
  color: $color-primary;
}

.icon-name {
  color: $text-secondary;
  font-size: 13px;
}
</style>

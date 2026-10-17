<template>
  <div class="app-container">
    <el-alert
      title="菜单配置已保存到服务端，所有管理员共享。排序值越小越靠前；隐藏后不在侧边栏展示；“更多管理”中的菜单从侧边栏底部入口进入。角色可见范围仍由权限管理控制。"
      type="info"
      :closable="false"
      show-icon
      class="mb-20"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <span class="toolbar-title">菜单配置</span>
        <div class="toolbar-actions">
          <el-button
            icon="el-icon-plus"
            :disabled="!availableRoutes.length"
            @click="handleAdd"
          >
            新增配置
          </el-button>
          <el-button icon="el-icon-refresh-left" @click="handleReset">
            恢复默认
          </el-button>
        </div>
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
        <el-table-column label="展示位置" width="100">
          <template slot-scope="{ row }">
            <el-tag
              :type="row.hidden ? 'info' : row.more ? 'warning' : 'success'"
              size="mini"
            >
              {{ row.hidden ? '已隐藏' : row.more ? '更多管理' : '侧边栏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="small"
              :disabled="!row.id"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="text"
              size="small"
              class="danger-text"
              :disabled="!row.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="isCreate ? '新增菜单配置' : '编辑菜单'"
      :visible.sync="editVisible"
      width="480px"
      append-to-body
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item v-if="isCreate" label="路由">
          <el-select
            v-model="editForm.key"
            placeholder="请选择尚未配置的路由"
            style="width: 100%"
            @change="handleRouteChange"
          >
            <el-option
              v-for="route in availableRoutes"
              :key="route.key"
              :label="`${route.title}（${route.key}）`"
              :value="route.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="路由">
          <span class="form-text">{{ editForm.name }}</span>
        </el-form-item>
        <el-form-item label="路由路径">
          <span class="form-text">{{ editForm.path || '-' }}</span>
        </el-form-item>
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
          <el-input-number v-model="editForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="显示">
          <el-switch v-model="editForm.visible" />
        </el-form-item>
        <el-form-item label="更多管理">
          <el-switch v-model="editForm.more" :disabled="!editForm.visible" />
          <span class="form-tip">开启后不在侧边栏展示，移至“更多管理”</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">
          保存
        </el-button>
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
  'el-icon-download',
  'el-icon-upload2',
  'el-icon-notebook-2',
  'el-icon-menu',
  'el-icon-box',
  'el-icon-setting'
]

export default {
  name: 'SystemMenuView',
  data() {
    return {
      saving: false,
      editVisible: false,
      isCreate: false,
      editForm: {
        id: null,
        key: '',
        name: '',
        path: '',
        title: '',
        icon: '',
        sort: 0,
        visible: true,
        more: false
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
    },
    availableRoutes() {
      return this.rootChildren
        .filter(route => route.name && !this.settings[route.name])
        .map(route => ({
          key: route.name,
          path: route.path,
          title: (route.meta && route.meta.title) || route.name,
          meta: route.meta || {}
        }))
    }
  },
  methods: {
    buildRows(routes, basePath = '') {
      return routes
        .map(route => {
          const key = route.name || route.path
          const config = this.settings[key] || {}
          const meta = route.meta || {}
          const fullPath = route.path.startsWith('/')
            ? route.path
            : `${basePath}/${route.path}`.replace(/\/+/g, '/')
          const hasCustomSort =
            config.sort !== undefined &&
            config.sort !== null &&
            config.sort !== ''
          const hasMoreConfig =
            config.more !== undefined && config.more !== null

          return {
            id: config.id || null,
            key,
            path: fullPath,
            title: config.title || meta.title || '',
            icon: config.icon || meta.icon || '',
            sort: hasCustomSort
              ? Number(config.sort) || 0
              : Number(meta.sort) || 0,
            hidden: config.hidden === true,
            more: hasMoreConfig ? config.more === true : meta.more === true,
            children: route.children
              ? this.buildRows(route.children, fullPath)
              : undefined
          }
        })
        .sort((a, b) => a.sort - b.sort)
    },
    handleAdd() {
      if (!this.availableRoutes.length) {
        this.$message.warning('所有路由都已配置菜单')
        return
      }
      this.isCreate = true
      this.editForm = {
        id: null,
        key: '',
        name: '',
        path: '',
        title: '',
        icon: 'el-icon-setting',
        sort: 0,
        visible: true,
        more: false
      }
      this.editVisible = true
    },
    handleRouteChange(key) {
      const route = this.availableRoutes.find(item => item.key === key)
      if (!route) {
        return
      }
      this.editForm.name = route.key
      this.editForm.path = route.path
      this.editForm.title = route.title
      this.editForm.icon = (route.meta && route.meta.icon) || 'el-icon-setting'
      this.editForm.sort = Number(route.meta && route.meta.sort) || 0
      this.editForm.more = Boolean(route.meta && route.meta.more)
    },
    handleEdit(row) {
      if (!row.id) {
        this.$message.warning('该菜单尚未保存到服务端，请先恢复默认或刷新')
        return
      }
      this.isCreate = false
      this.editForm = {
        id: row.id,
        key: row.key,
        name: row.key,
        path: row.path,
        title: row.title,
        icon: row.icon,
        sort: Number(row.sort) || 0,
        visible: !row.hidden,
        more: Boolean(row.more)
      }
      this.editVisible = true
    },
    saveEdit() {
      if (!this.editForm.title) {
        this.$message.warning('请输入菜单名称')
        return
      }
      if (this.isCreate && !this.editForm.key) {
        this.$message.warning('请选择路由')
        return
      }

      const payload = {
        title: this.editForm.title.trim(),
        icon: this.editForm.icon,
        sort: Number(this.editForm.sort) || 0,
        hidden: !this.editForm.visible,
        more: this.editForm.more === true
      }
      this.saving = true
      const action = this.isCreate
        ? this.$store.dispatch('menu/createMenu', {
          ...payload,
          name: this.editForm.name,
          path: this.editForm.path
        })
        : this.$store.dispatch('menu/saveMenu', {
          ...payload,
          id: this.editForm.id
        })

      action
        .then(() => {
          this.editVisible = false
          this.$message.success('菜单配置已保存')
        })
        .catch(() => {})
        .finally(() => {
          this.saving = false
        })
    },
    handleDelete(row) {
      if (!row.id) {
        return
      }
      this.$confirm(
        `确定删除“${row.title}”的菜单配置吗？删除后该菜单将按路由默认配置展示。`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
        .then(() => {
          return this.$store.dispatch('menu/deleteMenu', row.id)
        })
        .then(() => {
          this.$message.success('菜单配置已删除')
        })
        .catch(() => {})
    },
    handleReset() {
      this.$confirm('确定恢复默认菜单配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          return this.$store.dispatch('menu/resetMenus')
        })
        .then(() => {
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

.toolbar-actions {
  display: flex;
  gap: 8px;
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

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: $text-secondary;
}

.form-text {
  color: $text-primary;
}

.danger-text {
  color: $color-danger;
}
</style>

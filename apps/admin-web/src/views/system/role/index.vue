<template>
  <div class="app-container">
    <el-alert
      title="角色与菜单权限保存到后端；员工在「员工管理」中选择角色，下次登录后按角色显示菜单。"
      type="info"
      :closable="false"
      show-icon
      class="mb-20"
    />

    <el-card shadow="never">
      <div class="toolbar">
        <span class="toolbar-title">角色列表</span>
        <div class="toolbar-actions">
          <el-button icon="el-icon-refresh-left" @click="handleReset">
            恢复默认
          </el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
            新增角色
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="roles" border stripe>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="角色编码" min-width="140" />
        <el-table-column label="菜单权限" width="100">
          <template slot-scope="{ row }">
            {{ (row.menuPaths || []).length }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-switch
              :value="row.status"
              :active-value="1"
              :inactive-value="0"
              active-color="#67c23a"
              inactive-color="#f56c6c"
              @change="status => handleStatusChange(row, status)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="small" @click="handleAssign(row)">
              分配权限
            </el-button>
            <el-button
              v-if="row.code !== 'admin'"
              type="text"
              size="small"
              class="danger-text"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="form.id ? '编辑角色' : '新增角色'"
      :visible.sync="formVisible"
      width="460px"
      append-to-body
      @closed="handleFormClosed"
    >
      <el-form
        ref="roleForm"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="Boolean(form.id)"
            placeholder="如 admin"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRole">
          保存
        </el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="分配菜单权限"
      :visible.sync="permissionVisible"
      width="480px"
      append-to-body
    >
      <el-tree
        ref="menuTree"
        :data="menuTreeData"
        show-checkbox
        node-key="key"
        default-expand-all
        :props="{ label: 'label', children: 'children' }"
      />
      <span slot="footer">
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { constantRoutes } from '@/router/routes'
import {
  getRoleList,
  addRole,
  updateRole,
  deleteRole,
  enableOrDisableRole,
  resetRoles
} from '@/api/role'

export default {
  name: 'SystemRoleView',
  data() {
    const validateCode = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入角色编码'))
        return
      }
      if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(value)) {
        callback(new Error('编码需以字母开头，只能包含字母、数字和下划线'))
        return
      }
      const duplicate = this.roles.some(
        item => item.code === value && item.id !== this.form.id
      )
      if (duplicate) {
        callback(new Error('角色编码已存在'))
        return
      }
      callback()
    }

    return {
      loading: false,
      saving: false,
      roles: [],
      formVisible: false,
      form: {
        id: undefined,
        name: '',
        code: '',
        status: 1
      },
      rules: {
        name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        code: [{ validator: validateCode, trigger: 'blur' }]
      },
      permissionVisible: false,
      currentRole: null
    }
  },
  computed: {
    menuTreeData() {
      const root = constantRoutes.find(route => route.path === '/')
      const children = root && root.children ? root.children : []
      return this.buildMenuTree(children)
    }
  },
  created() {
    // 清理旧版本保存在前端的角色配置
    localStorage.removeItem('admin-web-roles')
    this.fetchRoles()
  },
  methods: {
    fetchRoles() {
      this.loading = true
      getRoleList()
        .then(res => {
          this.roles = res.data || []
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    buildMenuTree(routes) {
      return routes.map(route => ({
        key: route.name || route.path,
        label: (route.meta && route.meta.title) || route.path,
        children: route.children
          ? this.buildMenuTree(route.children)
          : undefined
      }))
    },
    handleAdd() {
      this.form = {
        id: undefined,
        name: '',
        code: '',
        status: 1
      }
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.roleForm) {
          this.$refs.roleForm.clearValidate()
        }
      })
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        name: row.name,
        code: row.code,
        status: row.status
      }
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.roleForm) {
          this.$refs.roleForm.clearValidate()
        }
      })
    },
    submitRole() {
      this.$refs.roleForm.validate(valid => {
        if (!valid || this.saving) {
          return
        }

        this.saving = true
        const isEdit = Boolean(this.form.id)
        const action = isEdit
          ? updateRole({ ...this.form })
          : addRole({ ...this.form })

        action
          .then(() => {
            this.$message.success(isEdit ? '角色修改成功' : '角色新增成功')
            this.formVisible = false
            this.fetchRoles()
          })
          .catch(() => {})
          .finally(() => {
            this.saving = false
          })
      })
    },
    handleFormClosed() {
      if (this.$refs.roleForm) {
        this.$refs.roleForm.clearValidate()
      }
    },
    handleStatusChange(row, status) {
      const previousStatus = row.status
      row.status = status
      enableOrDisableRole(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '角色已启用' : '角色已禁用')
        })
        .catch(() => {
          row.status = previousStatus
        })
    },
    handleAssign(row) {
      this.currentRole = row
      this.permissionVisible = true
      this.$nextTick(() => {
        if (this.$refs.menuTree) {
          this.$refs.menuTree.setCheckedKeys(row.menuPaths || [])
        }
      })
    },
    savePermissions() {
      if (!this.currentRole) {
        return
      }
      const checked = this.$refs.menuTree.getCheckedKeys()
      const halfChecked = this.$refs.menuTree.getHalfCheckedKeys()
      const menuPaths = Array.from(new Set([...checked, ...halfChecked]))

      updateRole({
        id: this.currentRole.id,
        menuPaths
      })
        .then(() => {
          this.permissionVisible = false
          this.$message.success('菜单权限已保存，员工下次登录后生效')
          this.fetchRoles()
        })
        .catch(() => {})
    },
    handleDelete(row) {
      if (row.code === 'admin') {
        this.$message.warning('超级管理员角色不能删除')
        return
      }
      this.$confirm(`确定删除角色“${row.name}”吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          deleteRole(row.id)
            .then(() => {
              this.$message.success('角色已删除')
              this.fetchRoles()
            })
            .catch(() => {})
        })
        .catch(() => {})
    },
    handleReset() {
      this.$confirm('确定恢复内置角色的默认菜单权限吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          resetRoles()
            .then(() => {
              this.$message.success('已恢复默认角色配置')
              this.fetchRoles()
            })
            .catch(() => {})
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
  gap: 10px;
}

.danger-text {
  color: $color-danger;
}
</style>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form
          :inline="true"
          :model="queryForm"
          class="search-form"
          @submit.native.prevent
        >
          <el-form-item label="员工姓名">
            <el-input
              v-model="queryForm.name"
              placeholder="请输入员工姓名"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              @click="handleSearch"
            >
              查询
            </el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">
              重置
            </el-button>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button
            v-permission="'employee:add'"
            type="primary"
            icon="el-icon-plus"
            @click="handleAdd"
          >
            新增员工
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无员工数据"
      >
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="性别" width="80">
          <template slot-scope="{ row }">
            <dict-tag type="employee_sex" :value="row.sex" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-color="#67c23a"
              inactive-color="#f56c6c"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="small" @click="handleDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :page-sizes="[10, 20, 50]"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <user-form
      :visible.sync="formVisible"
      :employee="editingEmployee"
      @success="handleFormSuccess"
    />
    <user-detail
      :visible.sync="detailVisible"
      :employee="detailEmployee"
    />
  </div>
</template>

<script>
import {
  getEmployeePage,
  enableOrDisableEmployee,
  getEmployeeById
} from '@/api/employee'
import UserForm from './components/UserForm.vue'
import UserDetail from './components/UserDetail.vue'
import DictTag from '@/components/DictTag/index.vue'

export default {
  name: 'SystemUserView',
  components: {
    UserForm,
    UserDetail,
    DictTag
  },
  data() {
    return {
      queryForm: {
        name: ''
      },
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      formVisible: false,
      editingEmployee: null,
      detailVisible: false,
      detailEmployee: null
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      getEmployeePage({
        name: this.queryForm.name.trim(),
        page: this.page,
        pageSize: this.pageSize
      })
        .then(res => {
          const data = res.data || {}
          this.list = data.records || []
          this.total = data.total || 0
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.page = 1
      this.fetchList()
    },
    handleReset() {
      this.queryForm.name = ''
      this.page = 1
      this.fetchList()
    },
    handleSizeChange(size) {
      this.page = 1
      this.pageSize = size
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.page = page
      this.fetchList()
    },
    handleAdd() {
      this.editingEmployee = null
      this.formVisible = true
    },
    handleEdit(row) {
      this.editingEmployee = { ...row }
      this.formVisible = true
    },
    handleDetail(row) {
      this.detailEmployee = { ...row }
      this.detailVisible = true

      getEmployeeById(row.id)
        .then(res => {
          if (res.data) {
            this.detailEmployee = res.data
          }
        })
        .catch(() => {})
    },
    handleStatusChange(row) {
      const status = row.status
      const previousStatus = status === 1 ? 0 : 1

      enableOrDisableEmployee(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '员工已启用' : '员工已禁用')
        })
        .catch(() => {
          row.status = previousStatus
        })
    },
    handleFormSuccess() {
      this.fetchList()
    },
    formatDateTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.search-form {
  flex: 1;
  min-width: 0;
}

.toolbar-actions {
  flex-shrink: 0;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
          <el-form-item label="分类名称">
            <el-input
              v-model="queryForm.name"
              placeholder="请输入分类名称"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item label="分类类型">
            <el-select v-model="queryForm.type" placeholder="全部" clearable>
              <el-option label="商品分类" :value="1" />
              <el-option label="套餐分类" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleSearch">
              查询
            </el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
          新增分类
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column label="类型" width="110">
          <template slot-scope="{ row }">
            <el-tag :type="row.type === 2 ? 'warning' : 'primary'" size="mini">
              {{ row.type === 2 ? '套餐分类' : '商品分类' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
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
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">
              删除
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

    <el-dialog
      :title="form.id ? '编辑分类' : '新增分类'"
      :visible.sync="formVisible"
      width="460px"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" clearable />
        </el-form-item>
        <el-form-item label="分类类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="商品分类" :value="1" />
            <el-option label="套餐分类" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          保存
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCategoryPage,
  addCategory,
  updateCategory,
  deleteCategory,
  enableOrDisableCategory
} from '@/api/category'

export default {
  name: 'CategoryView',
  data() {
    return {
      queryForm: { name: '', type: '' },
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      formVisible: false,
      submitting: false,
      form: { id: undefined, name: '', type: 1, sort: 0 },
      rules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      getCategoryPage({
        name: this.queryForm.name.trim(),
        type: this.queryForm.type || undefined,
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
      this.queryForm = { name: '', type: '' }
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
      this.form = { id: undefined, name: '', type: 1, sort: 0 }
      this.formVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        name: row.name,
        type: row.type,
        sort: row.sort || 0
      }
      this.formVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const action = this.form.id
          ? updateCategory({ ...this.form })
          : addCategory({ ...this.form })
        action
          .then(() => {
            this.$message.success(this.form.id ? '分类修改成功' : '分类新增成功')
            this.formVisible = false
            this.fetchList()
          })
          .catch(() => {})
          .finally(() => {
            this.submitting = false
          })
      })
    },
    handleStatusChange(row) {
      const status = row.status
      const previous = status === 1 ? 0 : 1
      enableOrDisableCategory(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '分类已启用' : '分类已禁用')
        })
        .catch(() => {
          row.status = previous
        })
    },
    handleDelete(row) {
      this.$confirm(`确定删除分类“${row.name}”吗？`, '提示', {
        type: 'warning'
      })
        .then(() => {
          deleteCategory(row.id)
            .then(() => {
              this.$message.success('分类已删除')
              this.fetchList()
            })
            .catch(() => {})
        })
        .catch(() => {})
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

.pagination {
  margin-top: 20px;
  text-align: right;
}

.danger-text {
  color: $color-danger;
}
</style>

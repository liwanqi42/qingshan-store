<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
          <el-form-item label="券名称">
            <el-input
              v-model="queryForm.name"
              placeholder="请输入优惠券名称"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="queryForm.type" placeholder="全部" clearable>
              <el-option
                v-for="item in typeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="全部" clearable>
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="0" />
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
          新增优惠券
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无优惠券">
        <el-table-column prop="name" label="券名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template slot-scope="{ row }">
            <el-tag size="mini">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="180">
          <template slot-scope="{ row }">{{ couponRule(row) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="210">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="发放/领取/剩余/使用" width="200">
          <template slot-scope="{ row }">
            <span class="stock-text">
              {{ row.totalCount === 0 ? '不限量' : row.totalCount }}
              / {{ row.receivedCount || 0 }}
              / {{ row.totalCount === 0 ? '不限量' : row.remainCount }}
              / {{ row.usedCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="每人限领" width="90">
          <template slot-scope="{ row }">{{ row.limitPerUser }}</template>
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
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
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
      :title="form.id ? '编辑优惠券' : '新增优惠券'"
      :visible.sync="formVisible"
      width="620px"
      append-to-body
      @closed="handleFormClosed"
    >
      <el-form
        ref="couponForm"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="券类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="item in typeOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 1 || form.type === 4" label="使用门槛" prop="fullMoney">
          <el-input-number v-model="form.fullMoney" :min="0" :precision="2" :step="10" />
          <span class="field-hint">元，满减券填 0 表示无门槛</span>
        </el-form-item>
        <el-form-item v-if="form.type === 1 || form.type === 3" label="减免金额" prop="minus">
          <el-input-number v-model="form.minus" :min="0.01" :precision="2" :step="1" />
          <span class="field-hint">元</span>
        </el-form-item>
        <el-form-item v-if="form.type === 2 || form.type === 4" label="折扣率" prop="rate">
          <el-input-number v-model="form.rate" :min="0.01" :max="0.99" :precision="2" :step="0.01" />
          <span class="field-hint">如 0.85 表示 8.5 折</span>
        </el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            class="date-picker"
          />
          <span class="field-hint">至</span>
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            class="date-picker"
          />
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="0" :precision="0" :step="100" />
          <span class="field-hint">张，0 表示不限量</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="limitPerUser">
          <el-input-number v-model="form.limitPerUser" :min="1" :precision="0" />
          <span class="field-hint">张</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          保存
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  addCoupon,
  deleteCoupon,
  getCouponPage,
  updateCoupon,
  updateCouponStatus
} from '@/api/coupon'

const EMPTY_FORM = {
  id: null,
  name: '',
  type: 1,
  fullMoney: 0,
  minus: 1,
  rate: 0.9,
  startTime: '',
  endTime: '',
  totalCount: 0,
  limitPerUser: 1,
  status: 0
}

export default {
  name: 'CouponView',
  data() {
    return {
      queryForm: {
        name: '',
        type: '',
        status: ''
      },
      typeOptions: [
        { value: 1, label: '满减券' },
        { value: 2, label: '折扣券' },
        { value: 3, label: '代金券' },
        { value: 4, label: '满折券' }
      ],
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      formVisible: false,
      submitting: false,
      form: { ...EMPTY_FORM },
      rules: {
        name: [
          { required: true, message: '请输入优惠券名称', trigger: 'blur' },
          { max: 64, message: '优惠券名称不能超过64个字符', trigger: 'blur' }
        ],
        type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
        fullMoney: [{ validator: this.validateThreshold, trigger: 'change' }],
        minus: [{ validator: this.validateMinus, trigger: 'change' }],
        rate: [{ validator: this.validateRate, trigger: 'change' }],
        totalCount: [{ required: true, message: '请填写发放总量', trigger: 'change' }],
        limitPerUser: [{ required: true, message: '请填写每人限领数量', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      getCouponPage({
        name: this.queryForm.name.trim(),
        type: this.queryForm.type === '' ? undefined : this.queryForm.type,
        status: this.queryForm.status === '' ? undefined : this.queryForm.status,
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
      this.queryForm = { name: '', type: '', status: '' }
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
      this.form = { ...EMPTY_FORM }
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.couponForm) {
          this.$refs.couponForm.clearValidate()
        }
      })
    },
    handleEdit(row) {
      this.form = {
        id: row.id,
        name: row.name,
        type: row.type,
        fullMoney: Number(row.fullMoney || 0),
        minus: Number(row.minus || 0),
        rate: Number(row.rate || 1),
        startTime: this.toPickerTime(row.startTime),
        endTime: this.toPickerTime(row.endTime),
        totalCount: row.totalCount || 0,
        limitPerUser: row.limitPerUser || 1,
        status: row.status
      }
      this.formVisible = true
      this.$nextTick(() => {
        if (this.$refs.couponForm) {
          this.$refs.couponForm.clearValidate()
        }
      })
    },
    handleFormClosed() {
      this.form = { ...EMPTY_FORM }
    },
    validateThreshold(rule, value, callback) {
      if (this.form.type !== 4) {
        callback()
        return
      }
      if (value === null || value === undefined || Number(value) <= 0) {
        callback(new Error('满折券的使用门槛必须大于0'))
        return
      }
      callback()
    },
    validateMinus(rule, value, callback) {
      if (this.form.type !== 1 && this.form.type !== 3) {
        callback()
        return
      }
      if (value === null || value === undefined || Number(value) <= 0) {
        callback(new Error('减免金额必须大于0'))
        return
      }
      callback()
    },
    validateRate(rule, value, callback) {
      if (this.form.type !== 2 && this.form.type !== 4) {
        callback()
        return
      }
      const rate = Number(value)
      if (!(rate > 0 && rate < 1)) {
        callback(new Error('折扣率必须在0到1之间'))
        return
      }
      callback()
    },
    submitForm() {
      this.$refs.couponForm.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.form.startTime || !this.form.endTime) {
          this.$message.error('请选择有效期')
          return
        }
        if (this.form.startTime >= this.form.endTime) {
          this.$message.error('开始时间必须早于结束时间')
          return
        }
        const type = this.form.type
        const payload = {
          id: this.form.id || undefined,
          name: this.form.name.trim(),
          type,
          fullMoney: type === 1 || type === 4 ? this.form.fullMoney : 0,
          minus: type === 1 || type === 3 ? this.form.minus : 0,
          rate: type === 2 || type === 4 ? this.form.rate : 1,
          startTime: this.form.startTime,
          endTime: this.form.endTime,
          totalCount: this.form.totalCount || 0,
          limitPerUser: this.form.limitPerUser || 1,
          status: this.form.status
        }
        this.submitting = true
        const request = this.form.id ? updateCoupon(payload) : addCoupon(payload)
        request
          .then(() => {
            this.$message.success(this.form.id ? '优惠券已更新' : '优惠券已创建')
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
      updateCouponStatus(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '优惠券已上架' : '优惠券已下架')
        })
        .catch(() => {
          row.status = previous
        })
    },
    handleDelete(row) {
      this.$confirm(`确定删除优惠券“${row.name}”吗？已发放的券不能删除。`, '提示', {
        type: 'warning'
      })
        .then(() => {
          deleteCoupon(row.id)
            .then(() => {
              this.$message.success('优惠券已删除')
              this.fetchList()
            })
            .catch(() => {})
        })
        .catch(() => {})
    },
    typeLabel(type) {
      const item = this.typeOptions.find(option => option.value === type)
      return item ? item.label : '-'
    },
    couponRule(row) {
      const money = value => Number(value || 0).toFixed(2)
      const rate = Number(row.rate || 0) * 10
      switch (row.type) {
        case 1:
          return Number(row.fullMoney || 0) > 0
            ? `满${money(row.fullMoney)}元减${money(row.minus)}元`
            : `无门槛减${money(row.minus)}元`
        case 2:
          return `${rate}折`
        case 3:
          return `${money(row.minus)}元代金券`
        case 4:
          return `满${money(row.fullMoney)}元享${rate}折`
        default:
          return '-'
      }
    },
    formatDateTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 16)
    },
    toPickerTime(value) {
      if (!value) return ''
      const text = String(value).replace('T', ' ')
      return text.length === 16 ? `${text}:00` : text.slice(0, 19)
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

.stock-text {
  color: #606266;
  font-size: 13px;
}

.field-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.date-picker {
  width: 190px;
}

.danger-text {
  color: #f56c6c;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>

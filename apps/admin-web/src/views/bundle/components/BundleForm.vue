<template>
  <el-dialog
    :title="form.id ? '编辑套餐' : '新增套餐'"
    :visible.sync="dialogVisible"
    width="760px"
    append-to-body
    @closed="handleClosed"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="套餐名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入套餐名称" clearable />
      </el-form-item>
      <el-form-item label="套餐分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="套餐价格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        <span class="price-hint">商品总价：¥{{ totalPrice.toFixed(2) }}</span>
      </el-form-item>
      <el-form-item label="套餐图片">
        <image-upload v-model="form.image" :type="1" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="请输入套餐描述"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">上架</el-radio>
          <el-radio :label="0">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="套餐商品">
        <div class="items-header">
          <span class="items-tip">请添加套餐包含的商品及份数</span>
          <el-button type="primary" size="mini" icon="el-icon-plus" @click="productDialogVisible = true">
            添加商品
          </el-button>
        </div>
        <el-table :data="items" border size="mini" class="items-table">
          <el-table-column prop="name" label="商品名称" min-width="140" />
          <el-table-column label="单价" width="100">
            <template slot-scope="{ row }">¥{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column label="份数" width="130">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.copies" :min="1" :max="99" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template slot-scope="{ row }">
              ¥{{ (Number(row.price) * Number(row.copies || 1)).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template slot-scope="{ $index }">
              <el-button type="text" size="mini" class="danger-text" @click="removeItem($index)">
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        保存
      </el-button>
    </span>

    <product-select-dialog
      :visible.sync="productDialogVisible"
      @confirm="handleProductSelected"
    />
  </el-dialog>
</template>

<script>
import {
  getBundleById,
  addBundle,
  updateBundle
} from '@/api/bundle'
import ImageUpload from '@/components/ImageUpload/index.vue'
import ProductSelectDialog from './ProductSelectDialog.vue'

export default {
  name: 'BundleForm',
  components: {
    ImageUpload,
    ProductSelectDialog
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    bundle: {
      type: Object,
      default: null
    },
    categories: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const validatePrice = (rule, value, callback) => {
      if (value === null || value === undefined || Number(value) <= 0) {
        callback(new Error('套餐价格必须大于 0'))
        return
      }
      callback()
    }

    return {
      form: {
        id: undefined,
        name: '',
        categoryId: '',
        price: 0,
        image: '',
        description: '',
        status: 0
      },
      items: [],
      rules: {
        name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
        categoryId: [
          { required: true, message: '请选择套餐分类', trigger: 'change' }
        ],
        price: [{ validator: validatePrice, trigger: 'blur' }]
      },
      productDialogVisible: false,
      submitting: false
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    totalPrice() {
      return this.items.reduce(
        (sum, item) =>
          sum + Number(item.price || 0) * Number(item.copies || 1),
        0
      )
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.handleOpen()
      }
    }
  },
  methods: {
    handleOpen() {
      if (this.bundle && this.bundle.id) {
        this.fillData(this.bundle)
        getBundleById(this.bundle.id)
          .then(res => {
            if (res.data) {
              this.fillData(res.data)
            }
          })
          .catch(() => {})
      } else {
        this.fillData(null)
      }

      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    fillData(data) {
      const bundle = data || {}
      this.form = {
        id: bundle.id,
        name: bundle.name || '',
        categoryId: bundle.categoryId || '',
        price: Number(bundle.price || 0),
        image: bundle.image || '',
        description: bundle.description || '',
        status: bundle.status === undefined ? 0 : bundle.status
      }
      this.items = (bundle.bundleItems || []).map(item => ({
        productId: item.productId,
        name: item.name,
        price: Number(item.price || 0),
        copies: Number(item.copies || 1)
      }))
    },
    handleClosed() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    handleProductSelected({ product, copies }) {
      const existing = this.items.find(item => item.productId === product.id)
      if (existing) {
        existing.copies = copies
        return
      }
      this.items.push({
        productId: product.id,
        name: product.name,
        price: Number(product.price || 0),
        copies
      })
    },
    removeItem(index) {
      this.items.splice(index, 1)
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.form.image) {
          this.$message.warning('请上传套餐图片')
          return
        }
        if (!this.items.length) {
          this.$message.warning('请至少添加一个套餐商品')
          return
        }
        if (this.items.some(item => Number(item.copies) < 1)) {
          this.$message.warning('商品份数至少为 1')
          return
        }

        this.submitting = true
        const payload = {
          id: this.form.id,
          name: this.form.name.trim(),
          categoryId: this.form.categoryId,
          price: Number(this.form.price),
          image: this.form.image,
          description: this.form.description,
          status: this.form.status,
          bundleItems: this.items.map(item => ({
            productId: item.productId,
            name: item.name,
            price: Number(item.price),
            copies: Number(item.copies)
          }))
        }

        const action = this.form.id
          ? updateBundle(payload)
          : addBundle(payload)

        action
          .then(() => {
            this.$message.success(this.form.id ? '套餐修改成功' : '套餐新增成功')
            this.dialogVisible = false
            this.$emit('success')
          })
          .catch(() => {})
          .finally(() => {
            this.submitting = false
          })
      })
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.price-hint {
  margin-left: 12px;
  font-size: 13px;
  color: $text-secondary;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 10px;
}

.items-tip {
  font-size: 13px;
  color: $text-secondary;
}

.items-table {
  width: 100%;
}

.danger-text {
  color: $color-danger;
}
</style>

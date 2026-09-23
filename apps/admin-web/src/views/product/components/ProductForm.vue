<template>
  <el-dialog
    :title="form.id ? '编辑商品' : '新增商品'"
    :visible.sync="dialogVisible"
    width="680px"
    append-to-body
    @closed="handleClosed"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入商品名称" clearable />
      </el-form-item>
      <el-form-item label="商品分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
      </el-form-item>
      <el-form-item label="封面图片">
        <image-upload v-model="form.image" :type="1" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="请输入商品描述"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">上架</el-radio>
          <el-radio :label="0">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="SKU 规格">
        <el-table :data="skuList" border size="mini" class="sku-table">
          <el-table-column label="规格">
            <template slot-scope="{ row }">
              {{ formatAttrs(row.attrs) }}
            </template>
          </el-table-column>
          <el-table-column label="价格" width="170">
            <template slot-scope="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0"
                :precision="2"
                size="mini"
              />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="150">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.stock" :min="0" size="mini" />
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
  </el-dialog>
</template>

<script>
import {
  getProductById,
  addProduct,
  updateProduct
} from '@/api/product'
import ImageUpload from '@/components/ImageUpload/index.vue'

function parseAttrs(attrs) {
  if (!attrs) {
    return {}
  }
  if (typeof attrs === 'string') {
    try {
      return JSON.parse(attrs)
    } catch (e) {
      return {}
    }
  }
  return attrs
}

export default {
  name: 'ProductForm',
  components: {
    ImageUpload
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    product: {
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
        callback(new Error('价格必须大于 0'))
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
      skuList: [],
      rules: {
        name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
        categoryId: [
          { required: true, message: '请选择商品分类', trigger: 'change' }
        ],
        price: [{ validator: validatePrice, trigger: 'blur' }]
      },
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
      if (this.product && this.product.id) {
        this.fillForm(this.product)
        getProductById(this.product.id)
          .then(res => {
            if (res.data) {
              this.fillForm(res.data)
            }
          })
          .catch(() => {})
      } else {
        this.fillForm(null)
      }

      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    fillForm(data) {
      const product = data || {}
      this.form = {
        id: product.id,
        name: product.name || '',
        categoryId: product.categoryId || '',
        price: Number(product.price || 0),
        image: product.image || '',
        description: product.description || '',
        status: product.status === undefined ? 0 : product.status
      }

      const skus = Array.isArray(product.skuList) ? product.skuList : []
      this.skuList = skus.length
        ? skus.map(sku => ({
            ...sku,
            price: Number(sku.price || 0),
            stock: Number(sku.stock || 0)
          }))
        : [
            {
              price: Number(product.price || 0),
              stock: Number(product.stock || 0),
              status: 1,
              attrs: {}
            }
          ]
    },
    handleClosed() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    formatAttrs(attrs) {
      const value = parseAttrs(attrs)
      const entries = Object.entries(value)
      if (!entries.length) {
        return '默认规格'
      }
      return entries.map(([key, item]) => `${key}:${item}`).join('，')
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.form.image) {
          this.$message.warning('请上传商品封面图片')
          return
        }
        if (!this.skuList.length) {
          this.$message.warning('请至少保留一个 SKU')
          return
        }
        const invalidSku = this.skuList.some(
          sku => Number(sku.price) <= 0 || Number(sku.stock) < 0
        )
        if (invalidSku) {
          this.$message.warning('SKU 价格必须大于 0，库存不能为负数')
          return
        }

        this.submitting = true
        const payload = {
          id: this.form.id,
          name: this.form.name.trim(),
          categoryId: this.form.categoryId,
          price: Number(this.form.price),
          stock: this.skuList.reduce(
            (sum, sku) => sum + (Number(sku.stock) || 0),
            0
          ),
          image: this.form.image,
          description: this.form.description,
          status: this.form.status,
          attrs: [],
          skuList: this.skuList.map(sku => ({
            id: sku.id,
            price: Number(sku.price),
            stock: Number(sku.stock) || 0,
            status: sku.status === undefined ? 1 : sku.status,
            attrs: parseAttrs(sku.attrs)
          }))
        }

        const action = this.form.id
          ? updateProduct(payload)
          : addProduct(payload)

        action
          .then(() => {
            this.$message.success(this.form.id ? '商品修改成功' : '商品新增成功')
            this.dialogVisible = false
            this.$emit('success')
          })
          .catch(() => {})
          .finally(() => {
            this.submitting = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.sku-table {
  width: 100%;
}
</style>

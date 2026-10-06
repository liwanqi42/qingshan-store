<template>
  <el-dialog
    :title="form.id ? '编辑商品' : '新增商品'"
    :visible.sync="dialogVisible"
    width="960px"
    append-to-body
    @closed="handleClosed"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="商品编号" prop="productCode">
            <el-input
              v-model="form.productCode"
              placeholder="如 3132，SKU 编号自动生成为 3132-1"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
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
        </el-col>
        <el-col :span="12">
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="封面图片" required>
        <image-upload v-model="form.image" :type="1" />
        <div class="field-tip">用于商品列表和详情页首图</div>
      </el-form-item>
      <el-form-item label="商品轮播图">
        <image-upload-list v-model="form.images" :type="2" :limit="6" />
      </el-form-item>
      <el-form-item label="详情长图">
        <image-upload-list v-model="form.detailImages" :type="3" :limit="20" />
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

      <el-form-item label="商品规格">
        <div class="spec-editor">
          <div
            v-for="(group, index) in specGroups"
            :key="group.key"
            class="spec-group"
          >
            <el-input
              v-model="group.name"
              class="spec-name"
              placeholder="规格名，如颜色"
              clearable
            />
            <el-select
              v-model="group.values"
              class="spec-values"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入规格值后按回车，如黑色"
            >
              <el-option
                v-for="value in group.values"
                :key="value"
                :label="value"
                :value="value"
              />
            </el-select>
            <el-button
              type="text"
              size="mini"
              class="danger-text"
              @click="removeSpecGroup(index)"
            >
              删除规格
            </el-button>
          </div>
          <div class="spec-actions">
            <el-button size="mini" icon="el-icon-plus" @click="addSpecGroup">
              添加规格
            </el-button>
            <span class="field-tip">
              修改规格会自动按规格组合刷新 SKU；无规格商品保留一个默认 SKU
            </span>
          </div>
          <div v-if="specIncomplete" class="spec-warning">
            请补全规格名和规格值，补全后会自动生成 SKU 组合
          </div>
        </div>
      </el-form-item>

      <el-form-item label="SKU 列表">
        <el-table :data="skuList" border size="mini" class="sku-table">
          <el-table-column label="编号" width="120">
            <template slot-scope="{ row }">
              <span v-if="row.skuCode">{{ row.skuCode }}</span>
              <span v-else class="placeholder-text">保存后生成</span>
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="120">
            <template slot-scope="{ row }">
              {{ formatAttrs(row.attrs) }}
            </template>
          </el-table-column>
          <el-table-column label="图片" width="100" align="center">
            <template slot-scope="{ row }">
              <image-upload v-model="row.image" :type="2" :size="56" />
            </template>
          </el-table-column>
          <el-table-column label="价格" width="140">
            <template slot-scope="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0"
                :precision="2"
                size="mini"
              />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.stock" :min="0" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template slot-scope="{ $index }">
              <el-button
                type="text"
                size="mini"
                class="danger-text"
                @click="removeSku($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!skuList.length" class="spec-warning">
          请至少保留一个 SKU
        </div>
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
import ImageUploadList from '@/components/ImageUploadList/index.vue'

let specKeySeed = 0

function nextSpecKey() {
  specKeySeed += 1
  return `spec-${specKeySeed}`
}

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

function parseAttrValues(attrValues) {
  if (Array.isArray(attrValues)) {
    return attrValues.filter(Boolean)
  }
  if (typeof attrValues === 'string') {
    try {
      const parsed = JSON.parse(attrValues)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (e) {
      return []
    }
  }
  return []
}

export default {
  name: 'ProductForm',
  components: {
    ImageUpload,
    ImageUploadList
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
        productCode: '',
        name: '',
        categoryId: '',
        price: 0,
        image: '',
        images: [],
        detailImages: [],
        description: '',
        status: 0
      },
      specGroups: [],
      skuList: [],
      specIncomplete: false,
      rules: {
        productCode: [
          { required: true, message: '请输入商品编号', trigger: 'blur' },
          {
            pattern: /^[0-9A-Za-z_-]+$/,
            message: '商品编号只能包含字母、数字、下划线和中划线',
            trigger: 'blur'
          }
        ],
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
    },
    specGroups: {
      deep: true,
      handler() {
        this.syncSkus()
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
        productCode: product.productCode || '',
        name: product.name || '',
        categoryId: product.categoryId || '',
        price: Number(product.price || 0),
        image: product.image || '',
        images: Array.isArray(product.images) ? product.images.filter(Boolean) : [],
        detailImages: Array.isArray(product.detailImages)
          ? product.detailImages.filter(Boolean)
          : [],
        description: product.description || '',
        status: product.status === undefined ? 0 : product.status
      }

      const attrs = Array.isArray(product.attrs) ? product.attrs : []
      this.specGroups = attrs
        .map(attr => ({
          key: nextSpecKey(),
          name: attr.name || '',
          values: parseAttrValues(attr.attrValues)
        }))
        .filter(group => group.name || group.values.length)

      const skus = Array.isArray(product.skuList) ? product.skuList : []
      this.skuList = skus.map(sku => ({
        id: sku.id,
        skuCode: sku.skuCode || '',
        attrs: parseAttrs(sku.attrs),
        price: Number(sku.price || 0),
        stock: Number(sku.stock || 0),
        image: sku.image || '',
        status: sku.status === undefined ? 1 : sku.status
      }))

      // 兼容只有SKU、没有规格表的旧数据：从SKU属性反推规格组
      if (!this.specGroups.length && this.skuList.length) {
        this.specGroups = this.deriveSpecGroups(this.skuList)
      }

      if (!this.skuList.length) {
        this.skuList = [this.createDefaultSku()]
      }
    },
    createDefaultSku() {
      return {
        id: undefined,
        skuCode: '',
        attrs: {},
        price: Number(this.form.price || 0),
        stock: 0,
        image: '',
        status: 1
      }
    },
    deriveSpecGroups(skuList) {
      const groups = []
      skuList.forEach(sku => {
        const attrs = parseAttrs(sku.attrs)
        Object.keys(attrs).forEach(name => {
          let group = groups.find(item => item.name === name)
          if (!group) {
            group = { key: nextSpecKey(), name, values: [] }
            groups.push(group)
          }
          const value = attrs[name]
          if (value !== undefined && value !== null && group.values.indexOf(value) === -1) {
            group.values.push(value)
          }
        })
      })
      return groups
    },
    addSpecGroup() {
      this.specGroups.push({ key: nextSpecKey(), name: '', values: [] })
    },
    removeSpecGroup(index) {
      this.specGroups.splice(index, 1)
    },
    removeSku(index) {
      this.skuList.splice(index, 1)
    },
    syncSkus() {
      const groups = this.specGroups.filter(
        group =>
          (group.name && group.name.trim()) ||
          (Array.isArray(group.values) && group.values.length)
      )

      // 有规格尚未填写完整时先不刷新，避免误清空已维护的价格库存
      const incomplete = groups.some(
        group =>
          !group.name ||
          !group.name.trim() ||
          !Array.isArray(group.values) ||
          !group.values.length
      )
      this.specIncomplete = incomplete
      if (incomplete) {
        return
      }

      if (!groups.length) {
        const hasDefaultSku =
          this.skuList.length === 1 &&
          Object.keys(parseAttrs(this.skuList[0].attrs)).length === 0
        if (!hasDefaultSku) {
          this.skuList = [this.createDefaultSku()]
        }
        return
      }

      const normalizedGroups = groups.map(group => ({
        name: group.name.trim(),
        values: group.values
      }))
      const existingBySignature = {}
      this.skuList.forEach(sku => {
        if (sku.id) {
          existingBySignature[this.comboSignature(sku.attrs, normalizedGroups)] = sku
        }
      })

      let combos = [[]]
      normalizedGroups.forEach(group => {
        const next = []
        combos.forEach(prefix => {
          group.values.forEach(value => {
            next.push(prefix.concat(value))
          })
        })
        combos = next
      })

      this.skuList = combos.map(values => {
        const attrs = {}
        normalizedGroups.forEach((group, index) => {
          attrs[group.name] = values[index]
        })
        const signature = values.join('|~|')
        const existing = existingBySignature[signature]
        if (existing) {
          return { ...existing, attrs }
        }
        return {
          id: undefined,
          skuCode: '',
          attrs,
          price: Number(this.form.price || 0),
          stock: 0,
          image: '',
          status: 1
        }
      })
    },
    comboSignature(attrs, groups) {
      const parsed = parseAttrs(attrs)
      return groups.map(group => parsed[group.name] || '').join('|~|')
    },
    validateSpecs() {
      const groups = this.specGroups.filter(
        group =>
          (group.name && group.name.trim()) ||
          (Array.isArray(group.values) && group.values.length)
      )
      if (!groups.length) {
        return true
      }
      if (
        groups.some(
          group =>
            !group.name ||
            !group.name.trim() ||
            !Array.isArray(group.values) ||
            !group.values.length
        )
      ) {
        this.$message.warning('请补全规格名和规格值')
        return false
      }
      const names = groups.map(group => group.name.trim())
      if (new Set(names).size !== names.length) {
        this.$message.warning('规格名不能重复')
        return false
      }
      return true
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
        if (!this.validateSpecs()) {
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
        const groups = this.specGroups.filter(
          group => group.name && group.name.trim() && group.values.length
        )
        const payload = {
          id: this.form.id,
          productCode: this.form.productCode.trim(),
          name: this.form.name.trim(),
          categoryId: this.form.categoryId,
          price: Number(this.form.price),
          stock: this.skuList.reduce(
            (sum, sku) => sum + (Number(sku.stock) || 0),
            0
          ),
          image: this.form.image,
          images: this.form.images.filter(Boolean),
          detailImages: this.form.detailImages.filter(Boolean),
          description: this.form.description,
          status: this.form.status,
          attrs: groups.map(group => ({
            name: group.name.trim(),
            attrValues: group.values
          })),
          skuList: this.skuList.map(sku => ({
            id: sku.id,
            price: Number(sku.price),
            stock: Number(sku.stock) || 0,
            status: sku.status === undefined ? 1 : sku.status,
            image: sku.image || null,
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
.field-tip {
  margin-left: 8px;
  color: $text-secondary;
  font-size: 12px;
}

.spec-editor {
  width: 100%;
}

.spec-group {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.spec-name {
  width: 150px;
  flex-shrink: 0;
}

.spec-values {
  flex: 1;
  min-width: 0;
}

.spec-actions {
  display: flex;
  align-items: center;
}

.spec-warning {
  margin-top: 6px;
  color: $color-danger;
  font-size: 12px;
}

.sku-table {
  width: 100%;
}

.placeholder-text {
  color: $text-secondary;
}

.danger-text {
  color: $color-danger;
}
</style>

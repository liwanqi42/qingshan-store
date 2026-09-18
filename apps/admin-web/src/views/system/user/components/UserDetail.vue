<template>
  <el-drawer
    title="员工详情"
    :visible.sync="drawerVisible"
    size="420px"
    append-to-body
  >
    <div v-if="employee" class="detail-content">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">
          {{ employee.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ employee.username || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ maskPhone(employee.phone) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ formatSex(employee.sex) }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">
          {{ maskIdNumber(employee.idNumber) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="Number(employee.status) === 1 ? 'success' : 'danger'">
            {{ Number(employee.status) === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(employee.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(employee.updateTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-drawer>
</template>

<script>
import { maskPhone, maskIdNumber } from '@/utils/mask'

export default {
  name: 'UserDetail',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    employee: {
      type: Object,
      default: null
    }
  },
  computed: {
    drawerVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    }
  },
  methods: {
    maskPhone,
    maskIdNumber,
    formatSex(sex) {
      if (sex === '1' || sex === 1) return '男'
      if (sex === '0' || sex === 0) return '女'
      return sex || '-'
    },
    formatDateTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-content {
  padding: 0 20px 20px;
}
</style>

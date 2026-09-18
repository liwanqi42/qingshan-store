<template>
  <el-dialog
    :title="isEdit ? '编辑员工' : '新增员工'"
    :visible.sync="dialogVisible"
    width="520px"
    append-to-body
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="请输入用户名"
          clearable
        />
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入姓名"
          clearable
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="form.phone"
          placeholder="请输入手机号"
          maxlength="11"
          clearable
        />
      </el-form-item>
      <el-form-item label="性别" prop="sex">
        <el-radio-group v-model="form.sex">
          <el-radio label="1">男</el-radio>
          <el-radio label="0">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="身份证号" prop="idNumber">
        <el-input
          v-model="form.idNumber"
          placeholder="请输入身份证号"
          clearable
        />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="loading"
        @click="submit"
      >
        确定
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { addEmployee, updateEmployee } from '@/api/employee'
import {
  isValidUsername,
  isValidPhone,
  isValidIdNumber
} from '@/utils/validate'

export default {
  name: 'UserForm',
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
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入用户名'))
        return
      }
      if (!isValidUsername(value)) {
        callback(new Error('用户名为 3-20 位字母、数字或下划线'))
        return
      }
      callback()
    }

    const validatePhone = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入手机号'))
        return
      }
      if (!isValidPhone(value)) {
        callback(new Error('手机号格式不正确'))
        return
      }
      callback()
    }

    const validateIdNumber = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入身份证号'))
        return
      }
      if (value.includes('*')) {
        callback()
        return
      }
      if (!isValidIdNumber(value)) {
        callback(new Error('身份证号格式不正确'))
        return
      }
      callback()
    }

    return {
      form: {
        id: undefined,
        username: '',
        name: '',
        phone: '',
        sex: '1',
        idNumber: ''
      },
      rules: {
        username: [{ validator: validateUsername, trigger: 'blur' }],
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
        ],
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
        idNumber: [{ validator: validateIdNumber, trigger: 'blur' }]
      },
      loading: false
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
    isEdit() {
      return Boolean(this.employee && this.employee.id)
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
      const employee = this.employee || {}
      this.form = {
        id: employee.id,
        username: employee.username || '',
        name: employee.name || '',
        phone: employee.phone || '',
        sex: String(employee.sex || '1'),
        idNumber: employee.idNumber || ''
      }
    },
    submit() {
      if (this.loading) {
        return
      }

      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }

        this.loading = true
        const action = this.isEdit
          ? updateEmployee({ ...this.form })
          : addEmployee({ ...this.form })

        action
          .then(() => {
            this.$message.success(this.isEdit ? '修改员工成功' : '新增员工成功')
            this.dialogVisible = false
            this.$emit('success')
          })
          .catch(() => {})
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

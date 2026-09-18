<template>
  <el-dialog
    title="修改密码"
    :visible.sync="dialogVisible"
    width="420px"
    append-to-body
    @open="handleOpen"
    @closed="resetForm"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="原密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入原密码"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          autocomplete="new-password"
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
import { editPassword } from '@/api/employee'
import { isValidPassword } from '@/utils/validate'

export default {
  name: 'PasswordDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    const validateNewPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入新密码'))
        return
      }
      if (!isValidPassword(value)) {
        callback(new Error('密码长度为 6-20 位'))
        return
      }
      if (value === this.form.oldPassword) {
        callback(new Error('新密码不能与原密码相同'))
        return
      }
      callback()
    }

    const validateConfirmPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
        return
      }
      if (value !== this.form.newPassword) {
        callback(new Error('两次输入的密码不一致'))
        return
      }
      callback()
    }

    return {
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { validator: validateNewPassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
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
    }
  },
  methods: {
    handleOpen() {
      this.resetForm()
    },
    resetForm() {
      this.form.oldPassword = ''
      this.form.newPassword = ''
      this.form.confirmPassword = ''
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
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
        editPassword({
          oldPassword: this.form.oldPassword,
          newPassword: this.form.newPassword
        })
          .then(() => {
            this.$message.success('密码修改成功，请重新登录')
            this.dialogVisible = false
            this.$store.dispatch('user/clearLoginState')
            this.$store.dispatch('tagsView/resetViews')
            this.$router.replace('/login')
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

<template>
  <el-tag v-if="item" :type="item.tagType || undefined" size="mini">
    {{ item.label }}
  </el-tag>
  <span v-else class="dict-tag-empty">{{ placeholder }}</span>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'DictTag',
  props: {
    type: {
      type: String,
      required: true
    },
    value: {
      type: [String, Number],
      default: ''
    },
    placeholder: {
      type: String,
      default: '-'
    }
  },
  computed: {
    ...mapGetters('dict', ['getDictItem']),
    item() {
      return this.getDictItem(this.type, this.value)
    }
  }
}
</script>

<style lang="scss" scoped>
.dict-tag-empty {
  color: $text-placeholder;
}
</style>

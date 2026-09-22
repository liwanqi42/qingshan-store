<template>
  <div ref="chart" class="chart-view" :style="{ height }" />
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'ChartView',
  props: {
    option: {
      type: Object,
      required: true
    },
    height: {
      type: String,
      default: '320px'
    }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart)
    this.chart.setOption(this.option)

    if (window.ResizeObserver) {
      this.resizeObserver = new window.ResizeObserver(() => {
        if (this.chart) {
          this.chart.resize()
        }
      })
      this.resizeObserver.observe(this.$refs.chart)
    }
    window.addEventListener('resize', this.handleResize)
  },
  watch: {
    option: {
      deep: true,
      handler(value) {
        if (this.chart) {
          this.chart.setOption(value, true)
        }
      }
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.chart-view {
  width: 100%;
}
</style>

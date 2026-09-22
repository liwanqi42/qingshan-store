<template>
  <div class="app-container report-page">
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <span class="filter-label">统计日期</span>
          <el-date-picker
            v-model="dateRange"
            class="date-picker"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :clearable="false"
            unlink-panels
            @change="handleQuery"
          />
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">
            查询
          </el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">
            重置
          </el-button>
        </div>
        <el-button
          type="success"
          icon="el-icon-download"
          :loading="exporting"
          @click="handleExport"
        >
          导出报表
        </el-button>
      </div>
    </el-card>

    <div v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <div class="chart-title">营业额统计</div>
            <chart-view :option="turnoverOption" height="320px" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="chart-card">
            <div class="chart-title">用户统计</div>
            <chart-view :option="userOption" height="320px" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card">
        <div class="summary-row">
          <div class="summary-item">
            <span>订单总数</span>
            <strong>{{ orderSummary.totalOrderCount }}</strong>
          </div>
          <div class="summary-item">
            <span>有效订单</span>
            <strong>{{ orderSummary.validOrderCount }}</strong>
          </div>
          <div class="summary-item">
            <span>订单完成率</span>
            <strong>{{ orderSummary.completionRate }}</strong>
          </div>
        </div>
        <div class="chart-title">订单统计</div>
        <chart-view :option="orderOption" height="320px" />
      </el-card>

      <el-card shadow="never" class="chart-card">
        <div class="chart-title">销量 Top10</div>
        <chart-view :option="top10Option" height="360px" />
      </el-card>
    </div>
  </div>
</template>

<script>
import {
  getTurnoverStatistics,
  getUserStatistics,
  getOrderStatistics,
  getTop10,
  exportReport
} from '@/api/report'
import ChartView from '@/components/Charts/ChartView.vue'
import { downloadBlob } from '@/utils/download'

function formatDate(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}`
}

function getDefaultRange() {
  const end = new Date()
  const begin = new Date()
  begin.setDate(end.getDate() - 6)
  return [formatDate(begin), formatDate(end)]
}

function splitList(value) {
  if (!value) {
    return []
  }
  return String(value)
    .split(',')
    .map(item => item.trim())
    .filter(item => item !== '')
}

function toNumbers(value) {
  return splitList(value).map(item => Number(item) || 0)
}

export default {
  name: 'StatisticsView',
  components: {
    ChartView
  },
  data() {
    return {
      dateRange: [],
      loading: false,
      exporting: false,
      turnoverData: {},
      userData: {},
      orderData: {},
      top10Data: {},
      orderSummary: {
        totalOrderCount: 0,
        validOrderCount: 0,
        completionRate: '0.0%'
      }
    }
  },
  computed: {
    turnoverOption() {
      const dates = splitList(this.turnoverData.dateList)
      return {
        tooltip: { trigger: 'axis' },
        grid: { left: 16, right: 24, top: 30, bottom: 10, containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLabel: { color: '#909399' },
          axisLine: { lineStyle: { color: '#dcdfe6' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [
          {
            name: '营业额',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            data: toNumbers(this.turnoverData.turnoverList),
            itemStyle: { color: '#409EFF' },
            areaStyle: { color: 'rgba(64, 158, 255, 0.12)' }
          }
        ]
      }
    },
    userOption() {
      const dates = splitList(this.userData.dateList)
      return {
        tooltip: { trigger: 'axis' },
        legend: { data: ['用户总量', '新增用户'], top: 0 },
        grid: { left: 16, right: 24, top: 40, bottom: 10, containLabel: true },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { color: '#909399' },
          axisLine: { lineStyle: { color: '#dcdfe6' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [
          {
            name: '用户总量',
            type: 'line',
            smooth: true,
            data: toNumbers(this.userData.totalUserList),
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '新增用户',
            type: 'line',
            smooth: true,
            data: toNumbers(this.userData.newUserList),
            itemStyle: { color: '#67C23A' }
          }
        ]
      }
    },
    orderOption() {
      const dates = splitList(this.orderData.dateList)
      return {
        tooltip: { trigger: 'axis' },
        legend: { data: ['订单数', '有效订单数'], top: 0 },
        grid: { left: 16, right: 24, top: 40, bottom: 10, containLabel: true },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { color: '#909399' },
          axisLine: { lineStyle: { color: '#dcdfe6' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [
          {
            name: '订单数',
            type: 'bar',
            barMaxWidth: 24,
            data: toNumbers(this.orderData.orderCountList),
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '有效订单数',
            type: 'line',
            smooth: true,
            data: toNumbers(this.orderData.validOrderCountList),
            itemStyle: { color: '#67C23A' }
          }
        ]
      }
    },
    top10Option() {
      const names = splitList(this.top10Data.nameList)
      const values = toNumbers(this.top10Data.numberList)
      return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 16, right: 32, top: 20, bottom: 10, containLabel: true },
        xAxis: {
          type: 'value',
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        yAxis: {
          type: 'category',
          data: names.slice().reverse(),
          axisLabel: {
            color: '#606266',
            width: 120,
            overflow: 'truncate'
          },
          axisLine: { lineStyle: { color: '#dcdfe6' } }
        },
        series: [
          {
            name: '销量',
            type: 'bar',
            barMaxWidth: 18,
            data: values.slice().reverse(),
            itemStyle: {
              color: '#409EFF',
              borderRadius: [0, 4, 4, 0]
            }
          }
        ]
      }
    }
  },
  created() {
    this.dateRange = getDefaultRange()
    this.fetchData()
  },
  methods: {
    fetchData() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        return
      }

      const params = {
        begin: this.dateRange[0],
        end: this.dateRange[1]
      }

      this.loading = true
      Promise.all([
        getTurnoverStatistics(params),
        getUserStatistics(params),
        getOrderStatistics(params),
        getTop10(params)
      ])
        .then(([turnover, users, orders, top10]) => {
          const orderData = orders.data || {}
          this.turnoverData = turnover.data || {}
          this.userData = users.data || {}
          this.orderData = orderData
          this.top10Data = top10.data || {}
          this.orderSummary = {
            totalOrderCount: orderData.totalOrderCount || 0,
            validOrderCount: orderData.validOrderCount || 0,
            completionRate: this.formatPercent(orderData.orderCompletionRate)
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    handleQuery() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请选择统计日期')
        return
      }
      this.fetchData()
    },
    handleReset() {
      this.dateRange = getDefaultRange()
      this.fetchData()
    },
    handleExport() {
      this.exporting = true
      exportReport()
        .then(response => {
          downloadBlob(response)
        })
        .catch(() => {})
        .finally(() => {
          this.exporting = false
        })
    },
    formatPercent(value) {
      return `${(Number(value || 0) * 100).toFixed(1)}%`
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: $text-regular;
}

.date-picker {
  width: 260px;
}

.chart-card {
  margin-bottom: 16px;
}

.chart-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.summary-row {
  display: flex;
  gap: 40px;
  margin-bottom: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;

  span {
    font-size: 13px;
    color: $text-secondary;
  }

  strong {
    margin-top: 6px;
    font-size: 22px;
    font-weight: 600;
    color: $text-primary;
  }
}

@media (max-width: 900px) {
  .date-picker {
    width: 100%;
  }

  .summary-row {
    gap: 20px;
    flex-wrap: wrap;
  }
}
</style>

#!/usr/bin/env bash
# ============================================================
# 性能基线冒烟脚本
#
# 前置条件：
#   1. 已用 mysql/schema.sql + seed-base.sql + seed-demo.sql 初始化独立测试库；
#   2. 已执行 scripts/perf-seed-orders.sql 生成 1 万笔订单；
#   3. 后端以 qingshan.datasource.database 指向该测试库启动。
#
# 用法：
#   BASE_URL=http://127.0.0.1:18082 ROUNDS=20 ./scripts/perf-smoke.sh
#
# 输出：商品列表、订单列表、报表导出的耗时（秒），以及报表文件大小。
# 说明：这是单机冒烟，不是压测；用于确认 1 万订单量级下接口没有明显退化。
# ============================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:18082}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
ROUNDS="${ROUNDS:-20}"
REPORT_FILE="${REPORT_FILE:-/tmp/qingshan-report.xlsx}"

login_response=$(curl -sS -m 10 -X POST "$BASE_URL/admin/employee/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
token=$(printf '%s' "$login_response" | sed -nE 's/.*"token":"([^"]+)".*/\1/p')
if [ -z "$token" ]; then
    echo "管理端登录失败：$login_response" >&2
    exit 1
fi
if printf '%s' "$login_response" | grep -q '"mustChangePassword":true'; then
    echo "测试账号仍需首次改密，无法访问管理端接口：" >&2
    echo "请先修改密码，或在独立测试库执行 UPDATE employee SET must_change_password=0 WHERE username='${ADMIN_USER}';" >&2
    exit 1
fi

summarize() {
    local name="$1"
    local unit="${2:-s}"
    shift 2
    local times=("$@")
    printf '%s\n' "${times[@]}" | awk -v name="$name" -v unit="$unit" '
        { n++; sum += $1; if (n == 1 || $1 < min) min = $1; if ($1 > max) max = $1 }
        END {
            if (n == 0) { print name ": 无数据"; exit }
            printf "%s: rounds=%d avg=%.3f%s min=%.3f%s max=%.3f%s\n",
                name, n, sum / n, unit, min, unit, max, unit
        }'
}

measure_json() {
    local name="$1"
    local url="$2"
    local -a times=()
    for _ in $(seq 1 "$ROUNDS"); do
        times+=("$(curl -sS -o /dev/null -w '%{time_total}' -m 60 "$url")")
    done
    summarize "$name" s "${times[@]}"
}

measure_json "商品列表 /user/product/page" \
    "$BASE_URL/user/product/page?page=1&pageSize=10"

# 管理端接口需要 token 请求头，单独测量
order_times=()
for _ in $(seq 1 "$ROUNDS"); do
    t=$(curl -sS -o /dev/null -w '%{time_total}' -m 60 \
        "$BASE_URL/admin/order/conditionSearch?page=1&pageSize=10" \
        -H "token: ${token}")
    order_times+=("$t")
done
summarize "订单列表 /admin/order/conditionSearch" s "${order_times[@]}"

report_sizes=()
report_times=()
for _ in $(seq 1 5); do
    result=$(curl -sS -o "$REPORT_FILE" -w '%{time_total} %{size_download} %{http_code}' -m 120 \
        "$BASE_URL/admin/report/export" -H "token: ${token}")
    read -r t size code <<< "$result"
    if [ "$code" != "200" ]; then
        echo "报表导出失败：HTTP $code" >&2
        exit 1
    fi
    report_times+=("$t")
    report_sizes+=("$size")
done
summarize "报表导出 /admin/report/export" s "${report_times[@]}"
summarize "报表文件大小（字节）" B "${report_sizes[@]}"

echo "报表文件：$REPORT_FILE"

import { formatSkuAttrs, formatDateTime, stockStatusLabel } from './stock'

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 生成入库/出库单打印 HTML
 * @param {Object} order 单据详情
 * @param {String} typeName 入库单/出库单
 */
export function buildStockOrderPrintHtml(order, typeName) {
  const items = order.items || []
  const totalQuantity = items.reduce(
    (sum, item) => sum + Number(item.quantity || 0),
    0
  )

  const rows = items
    .map(
      (item, index) => `
      <tr>
        <td class="center">${index + 1}</td>
        <td>${escapeHtml(item.productCode)}</td>
        <td>${escapeHtml(item.skuCode || '-')}</td>
        <td>${escapeHtml(item.productName)}</td>
        <td>${escapeHtml(formatSkuAttrs(item.skuAttrs))}</td>
        <td class="center">${escapeHtml(item.quantity)}</td>
        <td class="center">${
          item.stockBefore === null || item.stockBefore === undefined
            ? '-'
            : escapeHtml(item.stockBefore)
        }</td>
        <td class="center">${
          item.stockAfter === null || item.stockAfter === undefined
            ? '-'
            : escapeHtml(item.stockAfter)
        }</td>
      </tr>`
    )
    .join('')

  return `
    <div class="print-sheet">
      <h1>青山商店 ${escapeHtml(typeName)}</h1>
      <div class="meta">
        <div><span>单据编号：</span>${escapeHtml(order.orderNo)}</div>
        <div><span>单据状态：</span>${escapeHtml(stockStatusLabel(order.status))}</div>
        <div><span>制单人：</span>${escapeHtml(order.createUserName || '-')}</div>
        <div><span>制单时间：</span>${escapeHtml(formatDateTime(order.createTime))}</div>
        <div><span>审核人：</span>${escapeHtml(order.auditUserName || '-')}</div>
        <div><span>审核时间：</span>${escapeHtml(formatDateTime(order.auditTime))}</div>
      </div>
      <table>
        <thead>
          <tr>
            <th style="width: 6%">序号</th>
            <th style="width: 12%">商品编号</th>
            <th style="width: 14%">SKU编号</th>
            <th>商品名称</th>
            <th style="width: 22%">规格</th>
            <th style="width: 8%">数量</th>
            <th style="width: 8%">审核前</th>
            <th style="width: 8%">审核后</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
      <div class="summary">
        <span>共 ${items.length} 种商品</span>
        <span>合计数量：${totalQuantity}</span>
      </div>
      <div class="remark">备注：${escapeHtml(order.remark || '-')}</div>
      <div class="sign">
        <span>制单人：____________</span>
        <span>审核人：____________</span>
        <span>日期：____________</span>
      </div>
    </div>`
}

/**
 * 新窗口打印，避免打印样式影响后台页面
 * @param {String} title 标题
 * @param {String} bodyHtml 内容
 */
export function openPrintWindow(title, bodyHtml) {
  const win = window.open('', '_blank', 'width=1000,height=800')
  if (!win) {
    return false
  }

  win.document.open()
  win.document.write(`<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(title)}</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 24px;
      color: #303133;
      font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
      font-size: 13px;
    }
    h1 {
      margin: 0 0 18px;
      font-size: 22px;
      text-align: center;
      letter-spacing: 4px;
    }
    .meta {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px 24px;
      margin-bottom: 14px;
    }
    .meta span { color: #909399; }
    table {
      width: 100%;
      border-collapse: collapse;
      table-layout: fixed;
    }
    th, td {
      padding: 7px 8px;
      border: 1px solid #909399;
      word-break: break-all;
    }
    th { background: #f2f3f5; font-weight: 600; text-align: left; }
    .center { text-align: center; }
    .summary {
      display: flex;
      justify-content: flex-end;
      gap: 32px;
      margin-top: 12px;
      font-weight: 600;
    }
    .remark { margin-top: 14px; line-height: 1.7; }
    .sign {
      display: flex;
      justify-content: space-between;
      margin-top: 60px;
    }
    @page { size: A4; margin: 12mm; }
    @media print {
      body { padding: 0; }
    }
  </style>
</head>
<body>${bodyHtml}</body>
</html>`)
  win.document.close()
  win.focus()

  setTimeout(() => {
    win.print()
  }, 300)
  return true
}

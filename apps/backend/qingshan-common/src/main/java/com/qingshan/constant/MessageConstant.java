package com.qingshan.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String SYSTEM_ERROR = "系统繁忙，请稍后重试";
    public static final String DATA_CONFLICT = "数据已存在或不符合约束，请检查后重试";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_BUNDLE = "当前分类关联了捆绑包,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_PRODUCT = "当前分类关联了商品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String UPLOAD_TYPE_NOT_ALLOWED = "仅支持 jpg、jpeg、png、webp 格式的图片";
    public static final String UPLOAD_SIZE_EXCEEDED = "图片大小不能超过5MB";
    public static final String BUNDLE_ENABLE_FAILED = "捆绑包内包含未上架商品，无法上架";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String ROLE_NAME_REQUIRED = "请输入角色名称";
    public static final String ROLE_CODE_REQUIRED = "请输入角色编码";
    public static final String ROLE_CODE_INVALID = "角色编码需以字母开头，只能包含字母、数字和下划线";
    public static final String ROLE_CODE_EXISTS = "角色编码已存在";
    public static final String ROLE_CODE_IMMUTABLE = "角色编码不可修改";
    public static final String ROLE_NOT_FOUND = "角色不存在";
    public static final String ROLE_UNAVAILABLE = "角色不存在或已停用，请联系管理员";
    public static final String ROLE_IN_USE = "该角色下还有员工，不能删除";
    public static final String ADMIN_ROLE_PROTECTED = "内置管理员角色不能删除或禁用";
    public static final String ADMIN_EMPLOYEE_ROLE_IMMUTABLE = "管理员账号不能修改角色";
    public static final String NO_PERMISSION = "无权限执行该操作";
    public static final String PRODUCT_ON_SALE = "上架中的商品不能删除";
    public static final String BUNDLE_ON_SALE = "上架中的捆绑包不能删除";
    public static final String PRODUCT_BE_RELATED_BY_BUNDLE = "当前商品关联了捆绑包,不能删除";
    public static final String PRODUCT_STOCK_NOT_ENOUGH = "商品库存不足";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_REFUNDING = "该订单正在售后处理中，请先处理售后申请";
    public static final String REFUND_NOT_PENDING = "该订单没有待处理的售后申请";
    public static final String REFUND_REJECT_REASON_REQUIRED = "请填写拒绝售后原因";
    public static final String REFUND_TYPE_ONLY_BEFORE_SHIP = "商品未发货，仅支持仅退款";
    public static final String REFUND_TYPE_RETURN_AFTER_SHIP = "商品已发货，请选择退货退款、换货或补寄";
    public static final String REFUND_NOT_WAIT_RETURN = "该订单当前不在待买家退货状态";
    public static final String REFUND_NOT_RETURNED = "买家尚未退货，不能确认收货";
    public static final String REFUND_RETURN_LOGISTICS_REQUIRED = "请填写退货快递公司和物流单号";
    public static final String TIMEOUT_CANCEL_REASON = "超时未支付，系统自动取消";
    public static final String PRODUCT_STYLE_OFF_SHELF = "该商品样式已下架";
    public static final String BUNDLE_NOT_FOUND = "套装不存在";
    public static final String BUNDLE_OFF_SHELF = "该套装已下架";
    public static final String PRODUCT_NOT_FOUND = "商品不存在";
    public static final String PRODUCT_OFF_SHELF = "该商品已下架";
    public static final String PRODUCT_PRICE = "商品价格异常";
    public static final String ORDER_NAME_DESC = "青山商店订单";
    public static final String ORDER_ALREADY_PAID = "该订单已支付";
    public static final String ORDER_NEED_WECHAT_LOGIN = "当前账号未绑定微信，请使用微信登录后再支付";
    public static final String SHOP_CLOSED = "店铺休息中，暂不接单";
    public static final String PRODUCT_BUNDLE_INCOMPLETE = "商品或套装信息不完整";
    public static final String PRODUCT_BUY_LIMIT = "单个商品最多购买" + NumConstant.PRODUCT_MAX_BUY + "件";
    public static final String STOCK_ITEM_CODE_REQUIRED = "请输入商品编号或SKU编号";
    public static final String STOCK_ITEM_NOT_FOUND = "商品编号或SKU编号不存在";
    public static final String STOCK_ITEM_SKU_REQUIRED = "该商品有规格，请扫描 SKU 编号或先选择规格";
    public static final String STOCK_SKU_NOT_FOUND = "商品规格不存在";
    public static final String STOCK_SKU_MISMATCH = "商品与规格不匹配";
    public static final String STOCK_SKU_DISABLED = "该商品规格已禁用";
    public static final String STOCK_ITEM_DELETED = "商品已被删除，请删除该单据后重新录入";
    public static final String STOCK_ITEM_HAS_SKU = "商品已维护规格，请删除该明细后按 SKU 重新录入";
    public static final String STOCK_QUANTITY_INVALID = "数量必须为 1~999999 的整数";
    public static final String STOCK_ORDER_EMPTY = "请先添加商品明细";
    public static final String STOCK_ORDER_TOO_MANY_ITEMS = "单据明细不能超过 200 行";
    public static final String STOCK_ORDER_TYPE_INVALID = "单据类型不正确";
    public static final String STOCK_ORDER_NOT_FOUND = "单据不存在";
    public static final String STOCK_ORDER_STATUS_ERROR = "单据状态已变更，请刷新后重试";
    public static final String STOCK_ORDER_TYPE_IMMUTABLE = "不能修改单据类型";
    public static final String STOCK_ORDER_UPDATED = "只有待审核单据可以修改";
    public static final String STOCK_ORDER_DELETED = "只有待审核单据可以删除";
    public static final String STOCK_EXPORT_EMPTY = "没有可导出的数据";
    public static final String STOCK_LEDGER_EXPORT_EMPTY = "没有可导出的库存流水";


}

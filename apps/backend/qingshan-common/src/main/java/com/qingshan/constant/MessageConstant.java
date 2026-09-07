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
    public static final String PRODUCT_ON_SALE = "上架中的商品不能删除";
    public static final String BUNDLE_ON_SALE = "上架中的捆绑包不能删除";
    public static final String PRODUCT_BE_RELATED_BY_BUNDLE = "当前商品关联了捆绑包,不能删除";
    public static final String PRODUCT_STOCK_NOT_ENOUGH = "商品库存不足";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String TIMEOUT_CANCEL_REASON = "超时未支付，系统自动取消";
    public static final String PRODUCT_STYLE_OFF_SHELF = "该商品样式已下架";
    public static final String BUNDLE_NOT_FOUND = "套装不存在";
    public static final String BUNDLE_OFF_SHELF = "该套装已下架";
    public static final String PRODUCT_NOT_FOUND = "商品不存在";
    public static final String PRODUCT_OFF_SHELF = "该商品已下架";
    public static final String PRODUCT_PRICE = "商品价格异常";
    public static final String ORDER_NAME_DESC = "青山商店订单";
    public static final String ORDER_ALREADY_PAID = "该订单已支付";
    public static final String PRODUCT_BUNDLE_INCOMPLETE = "商品或套装信息不完整";
    public static final String PRODUCT_BUY_LIMIT = "单个商品最多购买" + NumConstant.PRODUCT_MAX_BUY + "件";


}

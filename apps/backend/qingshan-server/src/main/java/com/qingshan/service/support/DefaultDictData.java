package com.qingshan.service.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内置字典默认数据。
 *
 * 与 mysql/seed-base.sql 中的默认字典保持一致，修改任意一边时需同步另一边。
 */
public final class DefaultDictData {

    private static final List<Type> TYPES = Collections.unmodifiableList(Arrays.asList(
            type("employee_sex", "员工性别", 10,
                    item("男", "1", "", 1),
                    item("女", "0", "danger", 2)),
            type("employee_status", "员工状态", 20,
                    item("启用", "1", "success", 1),
                    item("禁用", "0", "danger", 2)),
            type("role_status", "角色状态", 30,
                    item("启用", "1", "success", 1),
                    item("禁用", "0", "danger", 2)),
            type("order_status", "订单状态", 40,
                    item("待付款", "1", "info", 1),
                    item("待接单", "2", "warning", 2),
                    item("待发货", "3", "warning", 3),
                    item("配送中", "4", "success", 4),
                    item("已完成", "5", "info", 5),
                    item("已取消", "6", "danger", 6),
                    item("退款", "7", "danger", 7)),
            type("order_refund_status", "售后状态", 50,
                    item("售后申请中", "1", "warning", 1),
                    item("待买家退货", "2", "info", 2),
                    item("待商家收货", "3", "info", 3),
                    item("售后完成", "4", "success", 4),
                    item("售后被拒绝", "5", "danger", 5),
                    item("售后关闭", "6", "info", 6)),
            type("order_refund_type", "售后类型", 60,
                    item("仅退款", "1", "warning", 1),
                    item("退货退款", "2", "warning", 2),
                    item("换货", "3", "info", 3),
                    item("补寄", "4", "info", 4)),
            type("pay_method", "支付方式", 70,
                    item("微信支付", "1", "success", 1),
                    item("支付宝", "2", "info", 2),
                    item("银行卡", "3", "info", 3)),
            type("category_type", "分类类型", 80,
                    item("商品分类", "1", "", 1),
                    item("套餐分类", "2", "warning", 2)),
            type("category_status", "分类状态", 90,
                    item("启用", "1", "success", 1),
                    item("禁用", "0", "danger", 2)),
            type("product_status", "商品状态", 100,
                    item("上架", "1", "success", 1),
                    item("下架", "0", "info", 2)),
            type("bundle_status", "套餐状态", 110,
                    item("上架", "1", "success", 1),
                    item("下架", "0", "info", 2)),
            type("stock_order_type", "库存单据类型", 120,
                    item("入库", "1", "success", 1),
                    item("出库", "2", "warning", 2)),
            type("stock_order_status", "库存单据状态", 130,
                    item("待审核", "0", "warning", 1),
                    item("已审核", "1", "success", 2)),
            type("shop_status", "营业状态", 140,
                    item("营业中", "1", "success", 1),
                    item("打烊中", "0", "info", 2))
    ));

    private DefaultDictData() {
    }

    public static List<Type> types() {
        return TYPES;
    }

    /**
     * 是否为内置字典类型
     */
    public static boolean isSystem(String type) {
        if (type == null) {
            return false;
        }
        for (Type item : TYPES) {
            if (item.getCode().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static Type type(String code, String name, int sort, Item... items) {
        return new Type(code, name, sort, Arrays.asList(items));
    }

    private static Item item(String label, String value, String tagType, int sort) {
        return new Item(label, value, tagType, sort);
    }

    public static class Type {

        private final String code;
        private final String name;
        private final int sort;
        private final List<Item> items;

        Type(String code, String name, int sort, List<Item> items) {
            this.code = code;
            this.name = name;
            this.sort = sort;
            this.items = items;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public int getSort() {
            return sort;
        }

        public List<Item> getItems() {
            return items;
        }
    }

    public static class Item {

        private final String label;
        private final String value;
        private final String tagType;
        private final int sort;

        Item(String label, String value, String tagType, int sort) {
            this.label = label;
            this.value = value;
            this.tagType = tagType;
            this.sort = sort;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public String getTagType() {
            return tagType;
        }

        public int getSort() {
            return sort;
        }
    }
}

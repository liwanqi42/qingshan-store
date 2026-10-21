package com.qingshan.service.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内置菜单默认配置。
 *
 * 与 mysql/seed-base.sql 中的默认菜单保持一致，修改任意一边时需同步另一边。
 */
public final class DefaultMenuData {

    private static final List<MenuDefinition> MENUS = Collections.unmodifiableList(Arrays.asList(
            menu("DashboardView", "/dashboard", "工作台", "el-icon-s-home", 10, false, false),
            menu("StatisticsView", "/statistics", "数据统计", "el-icon-s-data", 20, false, false),
            menu("OrderView", "/order", "订单管理", "el-icon-s-order", 30, false, false),
            menu("StockInView", "/stock/in", "入库管理", "el-icon-download", 40, false, false),
            menu("StockOutView", "/stock/out", "出库管理", "el-icon-upload2", 50, false, false),
            menu("StockLedgerView", "/stock/ledger", "库存台账", "el-icon-notebook-2", 60, false, false),
            menu("CategoryView", "/category", "分类管理", "el-icon-menu", 70, false, false),
            menu("ProductView", "/product", "商品管理", "el-icon-s-goods", 80, false, false),
            menu("BundleView", "/bundle", "套餐管理", "el-icon-box", 90, false, false),
            menu("CouponView", "/coupon", "优惠券管理", "el-icon-s-ticket", 95, false, false),
            menu("SystemUserView", "/system/user", "员工管理", "el-icon-s-custom", 100, false, false),
            menu("SystemMenuView", "/system/menu", "菜单管理", "el-icon-s-grid", 110, false, true),
            menu("SystemRoleView", "/system/role", "权限管理", "el-icon-s-check", 120, false, true),
            menu("SystemDictView", "/system/dict", "字典管理", "el-icon-collection", 130, false, true),
            menu("ShopView", "/shop", "店铺管理", "el-icon-s-shop", 140, false, true)
    ));

    private DefaultMenuData() {
    }

    public static List<MenuDefinition> menus() {
        return MENUS;
    }

    private static MenuDefinition menu(String name, String path, String title, String icon,
                                      int sort, boolean hidden, boolean more) {
        return new MenuDefinition(name, path, title, icon, sort, hidden, more);
    }

    public static class MenuDefinition {

        private final String name;
        private final String path;
        private final String title;
        private final String icon;
        private final int sort;
        private final boolean hidden;
        private final boolean more;

        MenuDefinition(String name, String path, String title, String icon,
                       int sort, boolean hidden, boolean more) {
            this.name = name;
            this.path = path;
            this.title = title;
            this.icon = icon;
            this.sort = sort;
            this.hidden = hidden;
            this.more = more;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getTitle() {
            return title;
        }

        public String getIcon() {
            return icon;
        }

        public int getSort() {
            return sort;
        }

        public boolean isHidden() {
            return hidden;
        }

        public boolean isMore() {
            return more;
        }
    }
}

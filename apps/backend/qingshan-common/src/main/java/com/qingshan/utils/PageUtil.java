package com.qingshan.utils;

/**
 * 分页参数归一化，防止前端传入过大 pageSize 导致慢查询或内存占用
 */
public final class PageUtil {

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    /**
     * 页码小于 1 时按第一页处理
     */
    public static int pageNum(int pageNum) {
        return Math.max(pageNum, DEFAULT_PAGE_NUM);
    }

    /**
     * 每页数量非正数时使用默认值，超过上限时截断
     */
    public static int pageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

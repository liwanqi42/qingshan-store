package com.qingshan.utils;

/**
 * 日志脱敏工具：避免手机号、身份证号等个人信息明文落盘
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /**
     * 手机号脱敏：138****1234
     */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证号脱敏：保留前 6 位和后 4 位，中间用 * 代替
     */
    public static String idNumber(String idNumber) {
        if (idNumber == null || idNumber.length() <= 10) {
            return mask(idNumber);
        }
        return idNumber.substring(0, 6)
                + repeat('*', idNumber.length() - 10)
                + idNumber.substring(idNumber.length() - 4);
    }

    /**
     * 通用脱敏：保留首尾各 1 位，中间用 * 代替
     */
    public static String mask(String value) {
        if (value == null || value.length() <= 2) {
            return value == null ? null : "*";
        }
        return value.charAt(0) + repeat('*', value.length() - 2) + value.charAt(value.length() - 1);
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}

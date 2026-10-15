package com.qingshan.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 随机初始密码生成
 */
public final class PasswordUtil {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String ALL = UPPER + LOWER + DIGIT;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * 生成包含大写、小写、数字的随机密码
     *
     * @param length 密码长度，建议不小于 10
     */
    public static String randomPassword(int length) {
        int size = Math.max(length, 10);
        List<Character> chars = new ArrayList<>(size);
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGIT));
        for (int i = chars.size(); i < size; i++) {
            chars.add(randomChar(ALL));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder builder = new StringBuilder(size);
        for (Character c : chars) {
            builder.append(c);
        }
        return builder.toString();
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}

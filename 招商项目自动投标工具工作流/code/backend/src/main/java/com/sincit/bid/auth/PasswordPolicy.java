package com.sincit.bid.auth;

import com.sincit.bid.common.BusinessException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 密码策略：8-32 位，至少包含字母与数字，不允许空白字符。
 * 管理员重置密码时生成 12 位高强度临时密码。
 */
public final class PasswordPolicy {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SYMBOL = "@#$%&*";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            throw BusinessException.invalid("密码长度需为 8-32 位");
        }
        if (password.matches(".*\\s.*")) {
            throw BusinessException.invalid("密码不能包含空格");
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        if (!hasLetter || !hasDigit) {
            throw BusinessException.invalid("密码需同时包含字母和数字");
        }
    }

    public static String generate() {
        List<Character> chars = new ArrayList<Character>();
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGIT));
        chars.add(pick(SYMBOL));
        String all = UPPER + LOWER + DIGIT + SYMBOL;
        while (chars.size() < 12) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char pick(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}

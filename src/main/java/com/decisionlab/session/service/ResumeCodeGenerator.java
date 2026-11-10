package com.decisionlab.session.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 이어하기 코드 생성기. 형식은 XXXX-9999 (대문자 4자 + 하이픈 + 숫자 4자, 9자). 명세서 4.17
 * 경우의 수는 26^4 * 10^4 (약 45억)이라 겹칠 일은 드물지만, 저장할 때 중복 여부를 한 번 더 확인한다.
 */
@Component
public class ResumeCodeGenerator {

    public static final String FORMAT = "^[A-Z]{4}-[0-9]{4}$";

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(9);
        for (int i = 0; i < 4; i++) {
            code.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        code.append('-');
        for (int i = 0; i < 4; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}

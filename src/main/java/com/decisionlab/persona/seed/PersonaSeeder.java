package com.decisionlab.persona.seed;

import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앱 시작 시 personas 시드를 넣는다. 이미 있는 code는 건너뛰므로 여러 번 실행해도 안전하다.
 * 이미 들어간 행의 값은 바꾸지 않는다. 문구를 고친 뒤에는 DB 행을 지우거나 직접 수정해야 반영된다.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaSeeder implements ApplicationRunner {

    private final PersonaRepository personaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PersonaCode code : PersonaCode.values()) {
            if (personaRepository.findByCode(code).isPresent()) {
                continue;
            }
            personaRepository.save(seedOf(code));
            log.info("Persona seed inserted: {}", code);
        }
    }

    // enum에 값이 추가되면 switch가 컴파일 에러를 내서 시드 누락을 막는다.
    private Persona seedOf(PersonaCode code) {
        return switch (code) {
            case ACHIEVEMENT -> persona(code,
                    "성취의 나",
                    "성장, 목표, 역량",
                    "그 선택이 네가 원하는 목표에 실제로 가까워지는 길일까?",
                    "#E5F2FC");
            case STABILITY -> persona(code,
                    "안정의 나",
                    "경제, 시간, 위험",
                    "그 선택에 너에게 안정적인 일상을 가져다줄까?",
                    "#E8F9DF");
            case EXPLORATION -> persona(code,
                    "탐색의 나",
                    "가능성, 경험, 자율성",
                    "어떤 선택이 너를 목표에 더 가까워지게 할까?",
                    "#F9F5CC");
            case DILEMMA_MANAGER -> persona(code,
                    "딜레마 관리자",
                    "균형, 검증, 성찰",
                    "상황이 달라져도 같은 선택일까요?",
                    "#3A0000");
        };
    }

    private Persona persona(PersonaCode code, String name, String coreValue,
                            String sampleQuestion, String themeColor) {
        return Persona.builder()
                .code(code)
                .name(name)
                .coreValue(coreValue)
                .sampleQuestion(sampleQuestion)
                .themeColor(themeColor)
                .build();
    }
}

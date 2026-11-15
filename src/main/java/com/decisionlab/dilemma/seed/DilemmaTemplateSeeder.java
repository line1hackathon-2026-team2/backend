package com.decisionlab.dilemma.seed;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.dilemma.entity.DilemmaTemplate;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 앱 시작 시 dilemma_templates 시드를 넣는다. 이미 있는 dilemma_type은 건너뛰므로 여러 번 실행해도 안전하다.
 * 이미 들어간 행의 값은 바꾸지 않는다. 문구를 고친 뒤에는 DB 행을 지우거나 직접 수정해야 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DilemmaTemplateSeeder implements ApplicationRunner {

    private final DilemmaTemplateRepository dilemmaTemplateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DilemmaType type : DilemmaType.values()) {
            if (dilemmaTemplateRepository.findByDilemmaType(type).isPresent()) {
                continue;
            }
            dilemmaTemplateRepository.save(seedOf(type));
            log.info("DilemmaTemplate seed inserted: {}", type);
        }
    }

    // enum에 값이 추가되면 switch가 컴파일 에러를 내서 시드 누락을 막는다.
    private DilemmaTemplate seedOf(DilemmaType type) {
        return switch (type) {
            case VALUE_CONFLICT -> template(type,
                    "중요한 가치를 위해 일상의 안정을 포기해야 한다면?",
                    "[가상 상황] 지금 선택한 길이 가장 중요하게 여기는 가치에는 부합하지만, 그 과정에서 일상의 여유나 현실적인 안정성이 예상보다 크게 줄어들 수 있다는 사실을 알게 되었습니다. 그래도 지금의 판단은 그대로 유지될까요?",
                    "핵심 가치를 지키기 위해 다른 중요한 가치의 손실을 어디까지 감수할 수 있는지 검증",
                    List.of(
                            new MessageOption("o1", "일상의 여유와 현실적인 안정성이 예상보다 더 중요하게 느껴져요"),
                            new MessageOption("o2", "감수해야 할 부담이 생각했던 것보다 커서 망설여져요")
                    ));
            case OPPORTUNITY_COST -> template(type,
                    "눈앞의 확실한 기회를 내려놓아야 한다면?",
                    "[가상 상황] 현재 선택을 실행하는 동안, 지금 당장 시작할 수 있는 매력적인 대안 기회를 포기해야 하는 상황이 생겼습니다. 눈앞의 확실한 기회를 내려놓아야 한다면, 지금의 판단은 달라질까요?",
                    "현재 선택을 위해 포기해야 하는 기회비용을 실제로 감수할 의향이 있는지 검증",
                    List.of(
                            new MessageOption("o1", "지금 당장 잡을 수 있는 확실한 기회를 놓치는 게 아까워졌어요"),
                            new MessageOption("o2", "현재 선택의 결과보다 대안 기회의 장점이 더 크게 느껴져요")
                    ));
            case ALT_PATH -> template(type,
                    "다른 경로로도 원하는 가치를 얻을 수 있다면?",
                    "[가상 상황] 지금 선택한 경로가 아니더라도, 원래 얻고자 했던 핵심 가치와 경험을 쌓을 수 있는 다른 방법이 제안되었습니다. 꼭 현재의 방식이 아니어도 목표에 다가갈 수 있다면, 지금의 판단은 달라질까요?",
                    "원하는 가치를 얻는 방법이 반드시 현재 선택한 경로여야만 하는지 검증",
                    List.of(
                            new MessageOption("o1", "꼭 지금 선택한 방식이 아니어도 목표를 이룰 수 있을 것 같아요"),
                            new MessageOption("o2", "시간이나 비용 부담이 적은 다른 경로가 더 합리적으로 보여요")
                    ));
            case UNCERTAINTY -> template(type,
                    "기대했던 환경이나 조건이 보장되지 않는다면?",
                    "[가상 상황] 아직 충분히 확인하지 못했던 실제 환경이나 세부 조건이 기대했던 모습과 다를 수 있다는 이야기를 먼저 경험한 사람에게 들었습니다. 기대한 조건이 보장되지 않더라도 지금의 선택을 유지할 수 있을까요?",
                    "미확인 조건이나 환경의 불확실성이 현실화되었을 때도 선택이 유지되는지 검증",
                    List.of(
                            new MessageOption("o1", "아직 확인하지 못한 조건이 예상과 다를까 봐 걱정돼요"),
                            new MessageOption("o2", "실제 환경을 먼저 확인해 본 뒤에 결정하고 싶어졌어요")
                    ));
        };
    }

    private DilemmaTemplate template(DilemmaType type, String title, String scenarioText,
                                     String verificationGoal, List<MessageOption> shakenValueOptions) {
        return DilemmaTemplate.builder()
                .dilemmaType(type)
                .title(title)
                .scenarioText(scenarioText)
                .verificationGoal(verificationGoal)
                .shakenValueOptions(shakenValueOptions)
                .build();
    }
}

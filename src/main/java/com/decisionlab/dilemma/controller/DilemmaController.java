package com.decisionlab.dilemma.controller;

import com.decisionlab.dilemma.dto.DilemmaFollowupRequest;
import com.decisionlab.dilemma.dto.DilemmaFollowupResponse;
import com.decisionlab.dilemma.dto.DilemmaRespondRequest;
import com.decisionlab.dilemma.dto.DilemmaRespondResponse;
import com.decisionlab.dilemma.service.DilemmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/dilemma")
@RequiredArgsConstructor
public class DilemmaController {

    private final DilemmaService dilemmaService;

    /** #11 딜레마 응답 (S06, S07). 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @Operation(summary = "#11 딜레마 응답")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "딜레마 응답 처리 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 잘못된 선택값"),
            @ApiResponse(responseCode = "401", description = "세션 토큰 없음 또는 불일치"),
            @ApiResponse(responseCode = "403", description = "다른 세션 접근"),
            @ApiResponse(responseCode = "404", description = "세션 또는 딜레마 없음"),
            @ApiResponse(responseCode = "409", description = "허용되지 않은 상태 전이 또는 재검토 한도(1회) 초과"),
            @ApiResponse(responseCode = "410", description = "세션 만료"),
            @ApiResponse(responseCode = "502", description = "리포트 생성(LLM) 실패")
    })
    @PostMapping("/respond")
    public DilemmaRespondResponse respond(@PathVariable UUID sessionId,
                                          @Valid @RequestBody DilemmaRespondRequest request) {
        return dilemmaService.respond(sessionId, request);
    }

    /** #12 딜레마 후속 입력 (S07, S08). 유지 이유(keepReason) 또는 흔들린 가치(shakenValue)를 저장한다. */
    @Operation(summary = "#12 딜레마 후속 입력")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "딜레마 후속 입력 처리 성공"),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 입력값 길이 초과"),
            @ApiResponse(responseCode = "401", description = "세션 토큰 없음 또는 불일치"),
            @ApiResponse(responseCode = "403", description = "다른 세션 접근"),
            @ApiResponse(responseCode = "404", description = "세션, 딜레마 또는 딜레마 응답 없음"),
            @ApiResponse(responseCode = "409", description = "허용되지 않은 상태 전이"),
            @ApiResponse(responseCode = "410", description = "세션 만료"),
            @ApiResponse(responseCode = "502", description = "리포트 생성(LLM) 실패")
    })
    @PutMapping("/followup")
    public DilemmaFollowupResponse followup(@PathVariable UUID sessionId,
                                            @Valid @RequestBody DilemmaFollowupRequest request) {
        return dilemmaService.followup(sessionId, request);
    }
}

package com.decisionlab.persona.controller;

import com.decisionlab.persona.dto.PersonaListResponse;
import com.decisionlab.persona.dto.PersonaListResponse.PersonaItem;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.service.PersonaService;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PersonaController.class)
class PersonaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PersonaService personaService;

    // SessionTokenFilter 의존성
    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    @Test
    void 토큰_없이_명세서_4_4_형식으로_응답한다() throws Exception {
        when(personaService.getSelectablePersonas()).thenReturn(new PersonaListResponse(List.of(
                new PersonaItem(PersonaCode.ACHIEVEMENT, "성취의 나", "성장, 목표, 역량",
                        "그 선택이 네가 원하는 목표에 실제로 가까워지는 길일까?", "#E5F2FC")
        )));

        mockMvc.perform(get("/api/v1/personas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personas.length()").value(1))
                .andExpect(jsonPath("$.personas[0].code").value("ACHIEVEMENT"))
                .andExpect(jsonPath("$.personas[0].name").value("성취의 나"))
                .andExpect(jsonPath("$.personas[0].coreValue").value("성장, 목표, 역량"))
                .andExpect(jsonPath("$.personas[0].sampleQuestion").value("그 선택이 네가 원하는 목표에 실제로 가까워지는 길일까?"))
                .andExpect(jsonPath("$.personas[0].themeColor").value("#E5F2FC"));
    }
}

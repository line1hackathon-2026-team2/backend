package com.decisionlab.ai.llm;

import java.util.Map;

public interface LlmClient {

    <T> T call(String promptKey, Map<String, Object> variables, Class<T> responseType);
}

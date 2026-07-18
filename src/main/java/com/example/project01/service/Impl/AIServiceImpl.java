package com.example.project01.service.Impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.example.project01.config.AIConfig;
import com.example.project01.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIConfig aiConfig;


    @Override
    public String chat(String message) {
        return chatWithModel(message, aiConfig.getModel());
    }

    @Override
    public String chatWithModel(String message, String model) {
        try {
            Generation gen = new Generation();

            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(message)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(aiConfig.getApiKey())
                    .model(model)
                    .messages(Arrays.asList(userMessage))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            GenerationResult result = gen.call(param);

            if (result != null && result.getOutput() != null
                    && result.getOutput().getChoices() != null
                    && !result.getOutput().getChoices().isEmpty()) {
                return result.getOutput().getChoices().get(0).getMessage().getContent();
            }

            return "AI响应为空";

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用阿里云AI失败", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }

//    @Override
//    public String chatWithHistory(String message, String sessionId) {
//        return chatWithHistoryAndModel(message, aiConfig.getModel(), sessionId);
//    }
//
//    @Override
//    public String chatWithHistoryAndModel(String message, String model, String sessionId) {
//        try {
//            Generation gen = new Generation();
//
//            List<Message> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
//
//            Message userMessage = Message.builder()
//                    .role(Role.USER.getValue())
//                    .content(message)
//                    .build();
//
//            history.add(userMessage);
//
//            if (history.size() > 20) {
//                history = history.subList(history.size() - 20, history.size());
//            }
//
//            GenerationParam param = GenerationParam.builder()
//                    .apiKey(aiConfig.getApiKey())
//                    .model(model)
//                    .messages(history)
//                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                    .build();
//
//            GenerationResult result = gen.call(param);
//
//            if (result != null && result.getOutput() != null
//                    && result.getOutput().getChoices() != null
//                    && !result.getOutput().getChoices().isEmpty()) {
//                String aiResponse = result.getOutput().getChoices().get(0).getMessage().getContent();
//
//                Message aiMessage = Message.builder()
//                        .role(Role.ASSISTANT.getValue())
//                        .content(aiResponse)
//                        .build();
//                history.add(aiMessage);
//
//                return aiResponse;
//            }
//
//            return "AI响应为空";
//
//        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
//            log.error("调用阿里云AI失败", e);
//            return "AI服务调用失败: " + e.getMessage();
//        }
//    }
//
//    @Override
//    public void clearHistory(String sessionId) {
//        conversationHistory.remove(sessionId);
//        log.info("已清除会话历史: {}", sessionId);
//    }
}




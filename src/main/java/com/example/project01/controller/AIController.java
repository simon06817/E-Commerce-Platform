package com.example.project01.controller;


import com.example.project01.common.Result;
import com.example.project01.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Thin HTTP entry points for the Aliyun AI chat service.
 */
@Tag(name = "AI助手", description = "阿里云AI相关接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController{

    private final AIService aiService;

    @Operation(summary = "AI对话", description = "发送消息给AI并获取回复")
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.getMessage());
        return Result.success(response);
    }

    @Operation(summary = "AI对话(指定模型)", description = "发送消息给AI并获取回复，可指定模型")
    @PostMapping("/chat/model")
    public Result<String> chatWithModel(@RequestBody ChatModelRequest request) {
        String response = aiService.chatWithModel(request.getMessage(), request.getModel());
        return Result.success(response);
    }

    @Operation(summary = "快速测试", description = "GET方式快速测试AI对话")
    @GetMapping("/test")
    public Result<String> test(@RequestParam String message) {
        String response = aiService.chat(message);
        return Result.success(response);
    }

    @lombok.Data
    public static class ChatRequest {
        private String message;
    }

    @lombok.Data
    public static class ChatModelRequest {
        private String message;
        private String model;
    }
}

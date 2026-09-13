package com.example.project01.service;

/**
 * Chat abstraction over the Aliyun DashScope model.
 */
public interface AIService {

    String chat(String message);

    String chatWithModel(String message, String model);
}


package com.example.project01.service;

public interface AIService {

    String chat(String message);

    String chatWithModel(String message, String model);

//    String chatWithHistory(String message, String sessionId);
//
//    String chatWithHistoryAndModel(String message, String model, String sessionId);
//
//    void clearHistory(String sessionId);
}


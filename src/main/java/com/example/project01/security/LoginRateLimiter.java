package com.example.project01.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final int limitPerMinute;
    private final long windowMillis;

    public LoginRateLimiter(@Value("${app.security.login-limit-per-minute:30}") int limitPerMinute,
                            @Value("${app.security.login-window-ms:60000}") long windowMillis) {
        this.limitPerMinute = limitPerMinute;
        this.windowMillis = windowMillis;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > windowMillis) {
                deque.pollFirst();
            }
            if (deque.size() >= limitPerMinute) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}

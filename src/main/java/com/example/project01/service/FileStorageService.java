package com.example.project01.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded images and returns their public URL.
 */
public interface FileStorageService {

    String storeImage(MultipartFile file);
}

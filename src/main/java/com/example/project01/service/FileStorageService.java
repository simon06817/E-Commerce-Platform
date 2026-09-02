package com.example.project01.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeImage(MultipartFile file);
}

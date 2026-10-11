package com.ecommerce.platform.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded images and returns their public URL.
 */
public interface FileStorageService {

    String storeImage(MultipartFile file);
}

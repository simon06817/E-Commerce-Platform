package com.ecommerce.platform.service;

import org.springframework.web.multipart.MultipartFile;

/** 文件存储服务。 */
public interface FileStorageService {

    /** 保存上传图片并返回访问地址。 */
    String storeImage(MultipartFile file);
}

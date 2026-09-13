package com.example.project01.service.Impl;

import com.example.project01.config.UploadProperties;
import com.example.project01.common.BusinessException;
import com.example.project01.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded images on the local disk with generated file names.
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UploadProperties uploadProperties;

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file must not be empty");
        }
        String original = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(original);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("only jpg/jpeg/png/gif/webp images are allowed");
        }
        File dir = new File(uploadProperties.getDir());
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("cannot create upload directory");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension.toLowerCase();
        File target = new File(dir, filename);
        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (IOException e) {
            throw new IllegalStateException("save file failed", e);
        }
        return uploadProperties.getUrlPrefix() + filename;
    }
}

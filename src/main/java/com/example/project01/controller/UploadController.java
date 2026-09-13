package com.example.project01.controller;

import com.example.project01.common.Result;
import com.example.project01.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload endpoint for images used by product and user interfaces.
 */
@Tag(name = "Upload", description = "local image upload")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload image", description = "any authenticated user")
    @PostMapping("/image")
    @PreAuthorize("isAuthenticated()")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(fileStorageService.storeImage(file));
    }
}

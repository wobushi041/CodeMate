package com.wobushi041.codemate.controller;

import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口
 */
@RestController
@RequestMapping("/file")
public class FileController {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Resource
    private UserService userService;

    @Value("${codemate.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        userService.getLoginUserFromRequest(request);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 jpg、png、webp、gif 图片");
        }

        String extension = getExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;
        Path avatarPath = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path targetPath = avatarPath.resolve(filename).normalize();
        if (!targetPath.startsWith(avatarPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名非法");
        }

        try {
            Files.createDirectories(avatarPath);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }

        return ResultUtils.success(buildAvatarUrl(request, filename));
    }

    private String getExtension(String originalFilename, String contentType) {
        if (StringUtils.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                return extension;
            }
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String buildAvatarUrl(HttpServletRequest request, String filename) {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }
        url.append(request.getContextPath()).append("/uploads/avatar/").append(filename);
        return url.toString();
    }
}

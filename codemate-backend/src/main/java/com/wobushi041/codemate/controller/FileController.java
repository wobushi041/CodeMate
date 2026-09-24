package com.wobushi041.codemate.controller;

import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 头像文件允许的最大字节数（2 MB）
     */
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    /**
     * 允许上传的头像图片 MIME 类型集合
     */
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 头像文件本地存储目录路径
     */
    @Value("${codemate.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    /**
     * 上传用户头像文件接口
     *
     * @param file    上传的头像文件对象
     * @param request HTTP 请求对象
     * @return 头像文件可访问的完整 URL 地址
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 校验用户登录状态及上传文件的非空性、大小与 MIME 类型
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

        // 生成随机文件名并校验目标存储路径安全性，防止目录穿越
        String extension = getExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;
        Path avatarPath = Paths.get(avatarDir).toAbsolutePath().normalize();
        Path targetPath = avatarPath.resolve(filename).normalize();
        if (!targetPath.startsWith(avatarPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名非法");
        }

        // 创建本地存储目录并写入头像文件
        try {
            Files.createDirectories(avatarPath);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }

        // 构造并返回头像访问 URL
        return ResultUtils.success(buildAvatarUrl(request, filename));
    }

    /**
     * 根据原始文件名或 MIME 类型推断文件后缀名
     *
     * @param originalFilename 上传文件的原始文件名
     * @param contentType      上传文件的 MIME 内容类型
     * @return 带点号的小写文件扩展名
     */
    private String getExtension(String originalFilename, String contentType) {
        // 优先从原始文件名中提取合法图片扩展名
        if (StringUtils.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {
                return extension;
            }
        }

        // 降级根据 MIME 类型映射对应扩展名
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    /**
     * 根据当前 HTTP 请求站点信息拼接头像完整访问 URL
     *
     * @param request  HTTP 请求对象
     * @param filename 保存后的头像文件名
     * @return 头像静态资源完整访问 URL
     */
    private String buildAvatarUrl(HttpServletRequest request, String filename) {
        // 拼接协议、域名与非标准端口号
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }

        // 拼接应用上下文路径与头像相对路径
        url.append(request.getContextPath()).append("/uploads/avatar/").append(filename);
        return url.toString();
    }

}

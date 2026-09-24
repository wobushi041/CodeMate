package com.wobushi041.codemate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring MVC 跨域与静态资源映射配置
 *
 * @author wobushi041
 */
@SpringBootConfiguration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 头像静态资源本地存储目录
     */
    @Value("${codemate.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    /**
     * 配置全局跨域请求映射规则
     *
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置全路径跨域放行规则，支持携带 Cookie 与主流 HTTP 方法
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .maxAge(3600);
    }

    /**
     * 配置上传头像的本地文件系统静态资源映射
     *
     * @param registry 静态资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将头像目录规范化为本地 URI 路径并确保以斜杠结尾
        Path avatarPath = Paths.get(avatarDir).toAbsolutePath().normalize();
        String avatarLocation = avatarPath.toUri().toString();
        if (!avatarLocation.endsWith("/")) {
            avatarLocation = avatarLocation + "/";
        }

        // 注册头像访问 URL 前缀与本地物理目录映射
        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations(avatarLocation);
        log.info("头像静态资源目录: {}", avatarLocation);
    }

}

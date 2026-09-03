package com.wobushi041.matchsystem.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootConfiguration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${match.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //覆盖所有请求
        registry.addMapping("/**")
                //允许发送Cookie
                .allowCredentials(true)
                //允许放行的域名（必须是patterns，否则*会与allowCredentials冲突）
                .allowedOriginPatterns("*")
                //设置允许的方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                //跨域允许时间
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarPath = Paths.get(avatarDir).toAbsolutePath().normalize();
        String avatarLocation = avatarPath.toUri().toString();
        if (!avatarLocation.endsWith("/")) {
            avatarLocation = avatarLocation + "/";
        }
        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations(avatarLocation);
        log.info("头像静态资源目录: {}", avatarLocation);
    }

}

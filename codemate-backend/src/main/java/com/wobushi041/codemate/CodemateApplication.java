package com.wobushi041.codemate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot 应用程序启动类
 *
 * @author wobushi041
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.wobushi041.codemate.mapper")
public class CodemateApplication {

    /**
     * 应用程序主入口方法
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用程序
        SpringApplication.run(CodemateApplication.class, args);
    }

}

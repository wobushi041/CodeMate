package com.wobushi041.codemate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Knife4j OpenAPI 3 接口文档配置
 *
 * @author wobushi041
 */
@Configuration
@Profile({"dev", "test"})
public class Knife4jConfig {

    /**
     * 构建 OpenAPI 文档全局基础信息
     *
     * @return OpenAPI 全局文档配置实例
     */
    @Bean
    public OpenAPI openAPI() {
        // 组装接口文档标题、版本号、联系人与描述信息
        return new OpenAPI()
                .info(new Info()
                        .title("CodeMate 智能编程协同平台接口文档")
                        .version("1.0")
                        .contact(new Contact().name("041"))
                        .description("CodeMate 后端接口文档"));
    }

    /**
     * 构建默认接口分组并指定 Controller 扫描包路径
     *
     * @return 接口分组配置实例
     */
    @Bean
    public GroupedOpenApi userApi() {
        // 扫描 controller 包下的全部接口并归入 default 分组
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.wobushi041.codemate.controller")
                .build();
    }

}

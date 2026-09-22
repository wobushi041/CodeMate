package com.wobushi041.codemate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.Serializable;

/**
 * knife4j配置类,openapi3
 */
@Configuration
@Profile({"dev", "test"})
public class Knife4jConfig  {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeMate 智能编程协同平台接口文档")
                        .version("1.0")
                        .contact(new Contact().name("041"))
                        .description("CodeMate 后端接口文档"));
    }
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.wobushi041.codemate.controller")
              //.pathsToMatch("/**")
                .build();
    }


}

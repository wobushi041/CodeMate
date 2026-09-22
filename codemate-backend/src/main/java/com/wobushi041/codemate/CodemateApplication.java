package com.wobushi041.codemate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.wobushi041.codemate.mapper")
public class CodemateApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodemateApplication.class, args);
    }

}



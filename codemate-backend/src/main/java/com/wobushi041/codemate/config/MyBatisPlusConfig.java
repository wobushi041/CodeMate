package com.wobushi041.codemate.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置
 *
 * @author wobushi041
 */
@Configuration
@MapperScan("com.wobushi041.codemate.mapper")
public class MyBatisPlusConfig {

    /**
     * 构建并注册 MyBatis-Plus 物理分页拦截器
     *
     * @return MyBatis-Plus 拦截器实例
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建主拦截器并添加基于 MySQL 方言的分页内部拦截器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

}

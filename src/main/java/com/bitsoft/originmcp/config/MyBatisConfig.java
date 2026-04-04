package com.bitsoft.originmcp.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis configuration.
 * DataSource is auto-configured from application.properties.
 */
@Configuration
@MapperScan("com.bitsoft.originmcp.mapper")
public class MyBatisConfig {
}

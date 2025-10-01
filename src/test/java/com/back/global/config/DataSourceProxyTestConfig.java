package com.back.global.config;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@TestConfiguration
public class DataSourceProxyTestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        // 실제 DataSource 생성 (테스트용 H2)
        DataSource actualDataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();

        // Proxy로 감싸서 쿼리 카운팅
        return ProxyDataSourceBuilder
                .create(actualDataSource)
                .name("QueryCountDataSource")
                .logQueryBySlf4j(SLF4JLogLevel.INFO)
                .countQuery()
                .build();
    }
}
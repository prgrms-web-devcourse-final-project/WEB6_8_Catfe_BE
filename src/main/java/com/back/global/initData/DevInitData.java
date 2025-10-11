package com.back.global.initData;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
@Profile("default")
@RequiredArgsConstructor
public class DevInitData {
    private final DataSource dataSource;
    private final DevInitService devInitService;

    @Bean
    ApplicationRunner DevInitDataApplicationRunner() {
        return args -> {
            runDataSql();
            devInitService.init();
        };
    }

    private void runDataSql() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            Statement stmt = connection.createStatement();

            // post_category 테이블에 데이터 있는지 확인
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM post_category");
            rs.next();
            long count = rs.getLong(1);

            if (count == 0) {
                // 데이터가 없으면 data.sql 실행
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("data.sql"));
                populator.setContinueOnError(true);
                populator.setIgnoreFailedDrops(true);
                populator.execute(dataSource);

                System.out.println("✅ data.sql executed because table was empty!");
            } else {
                System.out.println("ℹ️ Skipped data.sql (already has data: " + count + ")");
            }

        } catch (Exception e) {
            System.err.println("⚠️ data.sql execution failed: " + e.getMessage());
        }
    }
}

package com.batteryplus.survey.config;

//2 DataSources + 2 JdbcTemplate

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "verinaDataSource")
    @ConditionalOnProperty(name = "app.datasource.verina.enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "app.datasource.verina")
    public DataSource verinaDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "verinaJdbcTemplate")
    @ConditionalOnProperty(name = "app.datasource.verina.enabled", havingValue = "true")
    public JdbcTemplate verinaJdbcTemplate(@Qualifier("verinaDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "stagingDataSource")
    @ConditionalOnProperty(name = "app.datasource.staging.enabled", havingValue = "true")
    @ConfigurationProperties(prefix = "app.datasource.staging")
    public DataSource stagingDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "stagingJdbcTemplate")
    @ConditionalOnProperty(name = "app.datasource.staging.enabled", havingValue = "true")
    public JdbcTemplate stagingJdbcTemplate(@Qualifier("stagingDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
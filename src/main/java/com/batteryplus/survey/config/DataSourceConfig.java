package com.batteryplus.survey.config;

//2 DataSources + 2 JdbcTemplate

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "verinaDataSource")
    @ConfigurationProperties(prefix = "app.datasource.verina")
    public DataSource verinaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "stagingDataSource")
    @ConfigurationProperties(prefix = "app.datasource.staging")
    public DataSource stagingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "verinaJdbcTemplate")
    public JdbcTemplate verinaJdbcTemplate(@Qualifier("verinaDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "stagingJdbcTemplate")
    public JdbcTemplate stagingJdbcTemplate(@Qualifier("stagingDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}

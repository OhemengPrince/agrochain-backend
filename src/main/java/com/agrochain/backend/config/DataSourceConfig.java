package com.agrochain.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String rawUrl;

    @Value("${spring.datasource.username}")
    private String defaultUsername;

    @Value("${spring.datasource.password}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        String jdbcUrl = rawUrl;
        String username = defaultUsername;
        String password = defaultPassword;

        // Railway (and Heroku-style platforms) hand Postgres connections over as a
        // plain "postgresql://user:pass@host:port/db" URI. The pgjdbc driver needs
        // a "jdbc:" prefix and doesn't understand "user:pass@" userinfo syntax at
        // all, so the credentials have to be parsed out and passed to Hikari
        // separately rather than left in the URL string.
        if (rawUrl != null && rawUrl.startsWith("postgresql://")) {
            URI uri = new URI(rawUrl);
            jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            if (uri.getUserInfo() != null) {
                String[] credentials = uri.getUserInfo().split(":", 2);
                username = credentials[0];
                password = credentials.length > 1 ? credentials[1] : "";
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        return new HikariDataSource(config);
    }
}

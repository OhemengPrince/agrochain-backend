package com.agrochain.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.net.URI;
import java.util.Properties;

/**
 * EnvironmentPostProcessor instances are instantiated via the SPI in
 * META-INF/spring.factories using a no-arg constructor, before the
 * ApplicationContext exists — so this can't be a @Component/managed bean
 * and can't have anything autowired into it.
 */
public class DatabaseUrlConverter implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("DATABASE_URL");
        if (url == null || !url.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = new URI(url);
            String cleanUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath() + "?sslmode=disable";

            Properties props = new Properties();
            props.put("spring.datasource.url", cleanUrl);
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":", 2);
                props.put("spring.datasource.username", userInfo[0]);
                if (userInfo.length > 1) {
                    props.put("spring.datasource.password", userInfo[1]);
                }
            }

            environment.getPropertySources().addFirst(new PropertiesPropertySource("railway-db", props));
        } catch (Exception e) {
            System.err.println("DatabaseUrlConverter: failed to parse DATABASE_URL (" + e.getMessage() + "), falling back to raw value");
        }
    }
}

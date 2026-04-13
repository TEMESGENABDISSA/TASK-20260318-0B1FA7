package com.anju.config;

import com.anju.common.BusinessException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class NacosStartupValidator implements ApplicationRunner {

    private final Environment environment;

    public NacosStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String profile = environment.getProperty("SPRING_PROFILES_ACTIVE", "");
        if (profile.contains("docker")) {
            String nacosAddr = environment.getProperty("NACOS_ADDR", "");
            if (nacosAddr == null || nacosAddr.isBlank()) {
                throw new BusinessException("NACOS_ADDR is required in docker profile");
            }
            // Lightweight runtime check to ensure local nacos endpoint is reachable.
            String healthUrl = "http://" + nacosAddr + "/nacos/actuator/health";
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(healthUrl).openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 500) {
                    throw new BusinessException("Nacos endpoint not healthy: " + healthUrl);
                }
            } catch (Exception ex) {
                throw new BusinessException("Failed to connect Nacos: " + healthUrl);
            }
        }
    }
}

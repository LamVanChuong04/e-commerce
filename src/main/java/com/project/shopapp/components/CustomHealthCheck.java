package com.project.shopapp.components;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomHealthCheck implements HealthIndicator {
    @Override
    public Health health() {
        // Implement your custom health check logic here
        try {
            Map<String, Object> details = new HashMap<>();
            //DOWN => 503
            String computerName = InetAddress.getLocalHost().getHostName();
            details.put("computerName", String.format("computerName: %s", computerName));
            return Health.up().withDetails(details).build();
            //return Health.up().withDetail("computerName", computerName).build();//code: 200
        } catch (Exception e) {
            //throw new RuntimeException(e);
            return Health.down()
                    .withDetail("Error", e.getMessage()).build();
        }

    }
}
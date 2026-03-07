package com.lgcns.bebee.notification.core.config;


import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.lgcns.bebee.notification.domain",
        "com.lgcns.bebee.common.data.event"
})
@EnableJpaRepositories(basePackages = {
        "com.lgcns.bebee.notification",
        "com.lgcns.bebee.common.data.event",
})
public class PersistenceConfig {
}

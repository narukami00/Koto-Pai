package com.kotopai.outing.config;

import com.kotopai.outing.engine.SplitEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Makes the pure-Java engine available to Spring services; the engine itself stays free of Spring. */
@Configuration(proxyBeanMethods = false)
public class OutingEngineConfig {

    @Bean
    SplitEngine splitEngine() {
        return SplitEngine.withDefaults();
    }
}

package com.kotopai.outing.config;

import com.kotopai.outing.adapter.StubFriendshipChecker;
import com.kotopai.outing.port.FriendshipChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OutingPortsConfig {

    private static final Logger log = LoggerFactory.getLogger(OutingPortsConfig.class);

    /** Fallback used only while no real friendship adapter exists. */
    @Bean
    @ConditionalOnMissingBean(FriendshipChecker.class)
    FriendshipChecker stubFriendshipChecker() {
        log.warn("Using StubFriendshipChecker: every pair of users counts as friends. Replace with the real adapter (M2-28).");
        return new StubFriendshipChecker();
    }
}

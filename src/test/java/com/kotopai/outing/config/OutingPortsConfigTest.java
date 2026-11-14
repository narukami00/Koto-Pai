package com.kotopai.outing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.outing.adapter.StubFriendshipChecker;
import com.kotopai.outing.port.FriendshipChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Checks the "use the stub only when nobody provided the real thing" rule without starting the whole app. */
class OutingPortsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    @Configuration(proxyBeanMethods = false)
    static class RealFriendshipConfig {
        @Bean
        FriendshipChecker realChecker() {
            return (a, b) -> false;
        }
    }

    @Test
    @DisplayName("With no real adapter, the stub is registered")
    void stubIsTheFallback() {
        runner.withUserConfiguration(OutingPortsConfig.class).run(context -> {
            assertThat(context).hasSingleBean(FriendshipChecker.class);
            assertThat(context.getBean(FriendshipChecker.class)).isInstanceOf(StubFriendshipChecker.class);
        });
    }

    @Test
    @DisplayName("When a real adapter exists, the stub steps aside")
    void realAdapterWins() {
        runner.withUserConfiguration(RealFriendshipConfig.class, OutingPortsConfig.class).run(context -> {
            assertThat(context).hasSingleBean(FriendshipChecker.class);
            assertThat(context.getBean(FriendshipChecker.class)).isNotInstanceOf(StubFriendshipChecker.class);
        });
    }
}

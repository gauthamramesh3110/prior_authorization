package com.lifeforce.payer.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock getClock() {
        return Clock.systemUTC();
    }

}

package com.example.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling   // activates InvoiceService.markOverdueInvoices()
public class TimeConfig {

    /** Business time zone for "today" and due-date checks. Replace with a fixed Clock in tests. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}

package searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${scraper.user-agent}")
    private String userAgent;

    @Value("${scraper.referrer}")
    private String referrer;

    @Bean
    public String userAgent() {
        return userAgent;
    }

    @Bean
    public String referrer() {
        return referrer;
    }
}

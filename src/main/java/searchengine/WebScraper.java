package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import searchengine.utils.DelayUtils;

import java.io.IOException;

@Component
public class WebScraper {

    @Value("${scraper.user-agent}")
    private String userAgent;

    @Value("${scraper.referrer}")
    private String referrer;

    public Document scrape(String url) throws IOException {
        // Добавляем задержку перед выполнением запроса
        DelayUtils.delayBetweenRequests();

        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer);

        return connection.get();
    }
}

package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.services.SiteCrawlingTask;
import searchengine.utils.DelayUtils;

import java.io.IOException;

@Component
public class WebScraper {

    private static final Logger logger = LoggerFactory.getLogger(WebScraper.class);

    @Value("${scraper.user-agent}")
    private String userAgent;

    @Value("${scraper.referrer}")
    private String referrer;

    public Document scrape(String url, Site site) throws IOException {
        // Добавляем задержку перед выполнением запроса
        DelayUtils.delayBetweenRequests(site);

        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer);

        // Обработка ситуации, когда не удается получить документ
        try {
            return connection.get();
        } catch (IOException e) {
            // Логирование ошибки
            logger.error("Error while scraping URL: {}", url, e);
            // Можно выбросить исключение или вернуть null в зависимости от логики приложения
            throw new RuntimeException("Error while scraping URL: " + url, e);
        }
    }
}


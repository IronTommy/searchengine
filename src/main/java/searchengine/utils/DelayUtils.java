package searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Site;
import searchengine.services.SiteCrawlingTask;

import java.util.Random;

public class DelayUtils {

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawlingTask.class);

    private static final Random random = new Random();

    public static void delayBetweenRequests(Site site) {
        try {
            // Генерируем случайное число в диапазоне от 500 мс до 5000 мс (0,5–5 секунд)
            int delay = 500 + random.nextInt(4500);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Обработка исключения, если поток был прерван
            logger.error("Error during site crawling task for site: {}", site.getUrl(), e);
        }
    }
}

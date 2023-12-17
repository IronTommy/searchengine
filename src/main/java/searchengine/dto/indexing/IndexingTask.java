package searchengine.dto.indexing;

import java.util.List;
import java.util.concurrent.RecursiveTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class IndexingTask extends RecursiveTask<List<String>> {

    private final String url;

    public IndexingTask(String url) {
        this.url = url;
    }

    @Override
    protected List<String> compute() {
        // Ваш код для обхода страниц и извлечения ссылок
        try {
            Document document = Jsoup.connect(url).get();
            Elements links = document.select("a[href]");

            // Ваши дополнительные действия по обработке ссылок

            // Возвращаем список URL страниц
            return links.eachAttr("abs:href");
        } catch (Exception e) {
            // Обработка ошибок, если не удалось получить страницу
            e.printStackTrace();
            return List.of();
        }
    }
}

package searchengine.dto.indexing;

import java.util.List;
import java.util.concurrent.RecursiveTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class IndexingRecursiveTask extends RecursiveTask<IndexingTaskResult> {
    private final String url;

    public IndexingRecursiveTask(String url) {
        this.url = url;
    }

    @Override
    protected IndexingTaskResult compute() {
        try {
            // Используем JSOUP для получения содержимого страницы
            Document document = Jsoup.connect(url).get();

            // Используем JSOUP для извлечения ссылок из HTML-тегов <a>
            Elements links = document.select("a[href]");

            // Ваши дополнительные действия по обработке ссылок

            // Извлекаем URL страниц
            List<String> pageUrls = links.eachAttr("abs:href");

            // Возвращаем результат выполнения задачи
            return new IndexingTaskResult(pageUrls);
        } catch (Exception e) {
            // Обработка ошибок, если не удалось получить страницу
            e.printStackTrace();
            return new IndexingTaskResult(List.of());
        }
    }
}

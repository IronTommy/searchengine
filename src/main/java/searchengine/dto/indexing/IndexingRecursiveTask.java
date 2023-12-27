package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.services.SiteCrawlingTask;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class IndexingRecursiveTask extends RecursiveTask<IndexingTaskResult> {

    private static final Logger logger = LoggerFactory.getLogger(IndexingRecursiveTask.class);

    private final URL siteUrl;
    private final Set<URL> visitedUrls;

    public IndexingRecursiveTask(URL siteUrl, Set<URL> visitedUrls) {
        this.siteUrl = siteUrl;
        this.visitedUrls = visitedUrls;
    }

    private void addLinksToGlobalList(Set<String> links) {
        // Предположим, у вас есть статический общий список в вашем приложении
        // GlobalLinkStorage.addLinks(links);
        System.out.println("Adding links to the global list: " + links);
    }

    @Override
    protected IndexingTaskResult compute() {
        try {
            // Выполняем запрос к странице
            Connection.Response response = Jsoup.connect(siteUrl.toString()).execute();

            // Получаем код ответа
            int statusCode = response.statusCode();

            // Если код ответа успешный (например, 200), продолжаем обход страницы
            if (statusCode == 200) {
                Document document = response.parse();

                // Извлекаем ссылки с страницы
                Elements links = document.select("a[href]");

                // Преобразуем Elements в List<String>
                List<String> linksList = links.eachAttr("abs:href");

                // Создаем новый список из списка ссылок
                Set<String> linksSet = new HashSet<>(linksList);

                // Преобразуем текущую страницу в URL
                URL currentUrl = new URL(siteUrl.toString());

                // Добавляем текущую страницу в посещенные
                visitedUrls.add(currentUrl);

                // Исключаем уже посещенные ссылки
                linksSet.removeAll(visitedUrls);

                // Добавляем найденные ссылки в общий список
                addLinksToGlobalList(linksSet);

                // Ваши дополнительные действия по обработке ссылок

                // Возвращаем результат выполнения задачи
                return new IndexingTaskResult(linksSet);

            } else {
                // Обработка случая, когда запрос не был успешным
                System.out.println("Failed to fetch page. Status code: " + statusCode);
                return new IndexingTaskResult();
            }
        } catch (HttpStatusException e) {
            // Обработка ошибок, связанных с HTTP-статусами
            int statusCode = e.getStatusCode();
            logger.error("HTTP status error while fetching page {}: {}", siteUrl, statusCode, e);
            return new IndexingTaskResult();
        } catch (IOException e) {
            // Обработка других ошибок при запросе страницы
            logger.error("Error while fetching page: {}", siteUrl, e);
            return new IndexingTaskResult();
        }

    }
}

package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        System.out.println("Adding links to the global list: " + links);
    }

    @Override
    protected IndexingTaskResult compute() {
        try {
            Connection.Response response = Jsoup.connect(siteUrl.toString()).execute();
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                Document document = response.parse();
                Elements links = document.select("a[href]");
                List<String> linksList = links.eachAttr("abs:href");
                Set<String> linksSet = new HashSet<>(linksList);
                URL currentUrl = new URL(siteUrl.toString());
                visitedUrls.add(currentUrl);
                linksSet.removeAll(visitedUrls);
                addLinksToGlobalList(linksSet);
                return new IndexingTaskResult(linksSet);

            } else {
                System.out.println("Failed to fetch page. Status code: " + statusCode);
                return new IndexingTaskResult();
            }
        } catch (HttpStatusException e) {
            int statusCode = e.getStatusCode();
            logger.error("HTTP status error while fetching page {}: {}", siteUrl, statusCode, e);
            return new IndexingTaskResult();
        } catch (IOException e) {
            logger.error("Error while fetching page: {}", siteUrl, e);
            return new IndexingTaskResult();
        }

    }
}

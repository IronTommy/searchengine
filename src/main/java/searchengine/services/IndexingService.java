package searchengine.services;

import java.net.URL;
import java.util.List;

public interface IndexingService {
    boolean isIndexingInProgress();
    void startIndexing() throws Exception;
    List<URL> crawlPages(String siteUrl);
}

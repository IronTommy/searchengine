package searchengine.services;

import java.net.URL;
import java.util.List;

public interface IndexingService {
    boolean isIndexingInProgress();
    void startIndexing() throws Exception;
    void stopIndexing();
    List<URL> crawlPages(String siteUrl);
}

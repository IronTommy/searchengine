package searchengine.services;

import java.util.List;

public interface IndexingService {
    boolean isIndexingInProgress();
    void startIndexing() throws Exception;
    List<String> crawlPages(String siteUrl);

}

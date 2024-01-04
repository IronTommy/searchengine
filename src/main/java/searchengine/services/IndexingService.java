package searchengine.services;

import searchengine.model.*;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface IndexingService {
    boolean isIndexingInProgress();
    void startIndexing() throws Exception;
    void stopIndexing();
    List<URL> crawlPages(String siteUrl);
    boolean indexPage(URL pageUrl);
    List<SearchResult> search(String query, String site, int offset, int limit);
    <T> List<Page> findMatchingPages(List<T> lemmas, String site, int offset, int limit);
    List<Index> findByLemmaInAndPageSiteUrl(List<Lemma> lemmas, String siteUrl);
    List<Map<String, String>> loadSiteConfigs();
}

package searchengine.services;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchResult;

import java.net.URL;
import java.util.List;

public interface IndexingService {
    boolean isIndexingInProgress();
    void startIndexing() throws Exception;
    void stopIndexing();
    List<URL> crawlPages(String siteUrl);
    boolean indexPage(URL pageUrl);
    List<SearchResult> search(String query, String site, int offset, int limit);
    <T> List<Page> findMatchingPages(List<T> lemmas, String site, int offset, int limit);
    List<Index> findByLemmaInAndPageSiteUrl(List<Lemma> lemmas, String siteUrl);
}

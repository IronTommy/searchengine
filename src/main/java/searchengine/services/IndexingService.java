package searchengine.services;

import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;

import java.net.URL;
import java.util.List;

public interface IndexingService {
    void stopIndexing();
    @Transactional
    void startIndexing();
    boolean isIndexingInProgress();
    List<URL> crawlPages(String siteUrl);
    boolean indexPage(URL pageUrl);
    List<SearchResult> search(String query, String site, int offset, int limit);
    <T> List<Page> findMatchingPages(List<T> lemmas, String site, int offset, int limit);
    List<Index> findByLemmaInAndPageSiteUrl(List<Lemma> lemmas, String siteUrl);

}

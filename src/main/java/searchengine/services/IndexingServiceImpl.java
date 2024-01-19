package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingRecursiveTask;
import searchengine.dto.indexing.IndexingTaskResult;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.TextAnalyzer;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {


    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private volatile AtomicBoolean stopIndexingFlag = new AtomicBoolean(false);

    private final Object lock = new Object();

    private final Environment environment;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final Set<URL> visitedUrls = new HashSet<>();

    @Autowired
    private SitesList sitesList;

    @Override
    @Transactional
    public void stopIndexing() {
        synchronized (lock) {
            stopIndexingFlag.set(true);
        }
    }

    @Override
    @Transactional
    public synchronized void startIndexing() {
        logger.info("startIndexing method is called");

        try {
            List<Site> sites = saveSites(sitesList.getSites());

            List<CompletableFuture<IndexingTaskResult>> futures = indexSitesAsync(sites, forkJoinPool);

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            while (!stopIndexingFlag.get() && !allOf.isDone()) {
                logger.info("Before allOf.join(), stopIndexingFlag: {}", stopIndexingFlag.get());
                allOf.join();
                logger.info("After allOf.join(), stopIndexingFlag: {}", stopIndexingFlag.get());

            }

            if (!stopIndexingFlag.get()) {
                processIndexingResults(sites, futures);
                logger.debug("Indexing process completed successfully");
            } else {
                logger.info("Indexing process was stopped by the user");
            }

        } catch (IndexingServiceImpl.IndexingStoppedException e) {
            logger.info("Indexing process was stopped by the user");
        } catch (Exception e) {
            logger.error("Unexpected error during indexing process", e);
        }
    }

    @Transactional
    List<Site> saveSites(List<Site> sites) {
        List<Site> savedSites = new ArrayList<>();
        sites.forEach(siteConfig -> {
            Site savedSite = createAndSaveSite(siteConfig);
            savedSites.add(savedSite);
            logger.info("Saved site: {}", savedSite);
        });
        logger.info("Total {} sites saved", savedSites.size());
        return savedSites;
    }

    @Transactional
    Site createAndSaveSite(Site siteConfig) {
        Site site = new Site();
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());

        site.setUrl(String.valueOf(siteConfig.getUrl()));

        site.initializeName("NAME");
        try {
            siteRepository.saveAndFlush(site);
            logger.info("Site saved: {}", site);
        } catch (Exception e) {
            logger.error("Error saving site", e);
            e.printStackTrace();
        }
        return site;
    }

    @Transactional
    List<CompletableFuture<IndexingTaskResult>> indexSitesAsync(List<Site> sites, ForkJoinPool forkJoinPool) {
        return sites.stream()
                .map(site -> CompletableFuture.supplyAsync(() -> {
                    try {
                        synchronized (lock) {
                            if (stopIndexingFlag.get()) {
                                throw new IndexingStoppedException("Indexing stopped by the user");
                            }
                            return forkJoinPool.invoke(new IndexingRecursiveTask(
                                    new URL(site.getUrl()), visitedUrls, stopIndexingFlag, 0));
                        }
                    } catch (MalformedURLException e) {
                        logger.error("Malformed URL for site: {}", site.getUrl(), e);
                        return null;
                    }
                }))
                .collect(Collectors.toList());
    }

    @Transactional
    void processIndexingResults(List<Site> sites, List<CompletableFuture<IndexingTaskResult>> futures) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.join();
            for (int i = 0; i < futures.size(); i++) {
                Site site = sites.get(i);
                CompletableFuture<IndexingTaskResult> future = futures.get(i);

                IndexingTaskResult taskResult = future.get();
                logger.info("Task result for site {}: {}", site.getUrl(), taskResult);

                Optional.ofNullable(taskResult)
                        .ifPresent(result -> processIndexingResult(site, result));

            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while processing indexing results", e);
        }
    }

    @Transactional
    public void processIndexingResult(Site site, IndexingTaskResult result) {
        logger.info("Processing indexing result for site: {}", site.getUrl());

        for (String pageUrl : result.getPageUrls()) {
            processPage(site, pageUrl);
        }

        updateSiteStatus(site);

        logger.info("Processing indexing result completed for site: {}", site.getUrl());
    }

    private boolean isValidHttpResponse(URL url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000)
                    .build();

            HttpGet httpGet = new HttpGet(url.toString());
            httpGet.setConfig(requestConfig);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode >= 200 && statusCode < 300;
            }
        } catch (Exception e) {
            logger.error("Error while checking HTTP response for URL: {}", url, e);
            return false;
        }
    }

    private void processPage(Site site, String pageUrl) {
        try {
            URL url = new URL(pageUrl);

            if (!isValidHttpResponse(url)) {
                logger.warn("Skipping indexing for {} due to error HTTP response", pageUrl);
                return;
            }

            String pageContent = getPageContent(url);
            List<String> lemmas = extractLemmas(pageContent);

            for (String lemmaText : lemmas) {
                saveLemmaToDatabase(site, lemmaText);
                logger.debug("Lemma saved for site: {}, text: {}", site.getUrl(), lemmaText);
            }

            Page page = savePageToDatabase(site, url.getPath());

            indexLemmas(site, lemmas, page);

            logger.info("Page added for site: {}", site.getUrl());
        } catch (MalformedURLException e) {
            logger.error("Malformed URL while processing page for site {}: {}", site.getUrl(), pageUrl, e);
        } catch (Exception e) {
            logger.error("Error processing page for site {}: {}", site.getUrl(), pageUrl, e);
        }
    }

    private void indexLemmas(Site site, List<String> lemmas, Page page) {
        for (String lemmaText : lemmas) {
            Optional<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaText);
            if (optionalLemma.isPresent()) {
                Lemma lemma = optionalLemma.get();

                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRankValue(0.8f);
                indexRepository.save(index);
                logger.info("Index saved: " + index);
                logger.debug("Index saved for site: {}, lemma: {}", site.getUrl(), lemmaText);
            } else {
                logger.error("Lemma not found for site {} and lemma {}", site.getUrl(), lemmaText);
            }
        }
    }

    private Page savePageToDatabase(Site site, String path) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        pageRepository.save(page);
        logger.debug("Page saved for site: {}", site.getUrl());
        return page;
    }

    private void updateSiteStatus(Site site) {
        site.setStatus(SiteStatus.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        site.initializeName("NAME");
        siteRepository.save(site);
        logger.debug("Site saved: {}", site);
    }

    private void saveLemmaToDatabase(Site site, String lemmaText) {
        try {
            Optional<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaText);

            if (optionalLemma.isPresent()) {
                Lemma existingLemma = optionalLemma.get();
                existingLemma.setFrequency(existingLemma.getFrequency() + 1);
                lemmaRepository.save(existingLemma);
                logger.info("Existing lemma saved: {}", existingLemma);
            } else {
                Lemma newLemma = new Lemma();
                newLemma.setSite(site);
                newLemma.setLemma(lemmaText);
                newLemma.setFrequency(1);
                lemmaRepository.save(newLemma);
                logger.info("New lemma saved: {}", newLemma);
            }
        } catch (Exception e) {
            logger.error("Error saving lemma to the database", e);
        }
    }

    @Override
    @Transactional
    public boolean isIndexingInProgress() {
        return !forkJoinPool.isTerminated();
    }

    @Override
    @Transactional
    public boolean indexPage(URL pageUrl) {
        try {
            Site site = siteRepository.findByUrlIgnoreCase(pageUrl.toString());

            boolean isSiteAllowed = isSiteAllowed(pageUrl.toString());
            if (!isSiteAllowed) {
                site = saveSiteAndUpdateStatus(site, pageUrl, SiteStatus.FAILED);
                return false;
            }

            site = saveSiteAndUpdateStatus(site, pageUrl, SiteStatus.INDEXING);
            boolean indexingResult = myIndexingLogic(pageUrl);

            saveSiteAndUpdateStatus(site, pageUrl, indexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED);

            Site finalSite = site;
            CompletableFuture.runAsync(() -> {
                try {
                    boolean asyncIndexingResult = yourAsyncIndexingLogic(pageUrl);
                    saveSiteAndUpdateStatus(finalSite, pageUrl, asyncIndexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED);
                } catch (Exception e) {
                    logger.error("Error indexing page asynchronously: " + pageUrl, e);
                }
            }).exceptionally(throwable -> {
                logger.error("Error indexing page asynchronously: " + pageUrl, throwable);
                return null;
            });

            return true;
        } catch (Exception e) {
            logger.error("Error indexing page: " + pageUrl, e);
            return false;
        }
    }

    private Site saveSiteAndUpdateStatus(Site site, URL pageUrl, SiteStatus status) {
        if (site == null) {
            site = new Site();
            site.setUrl(pageUrl.toString());
        }

        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());

        return siteRepository.save(site);
    }

    private boolean isSiteAllowed(String url) {
        logger.debug("Available sites from properties: " + environment.getProperty("indexing-settings.sites"));

        List<String> allowedSites = Optional.ofNullable(environment
                        .getProperty("indexing-settings.sites", List.class))
                .orElse(Collections.emptyList());

        url = url.toLowerCase();

        for (String allowedSite : allowedSites) {
            String lowerCaseSite = allowedSite.toLowerCase();

            boolean isAllowed = url.startsWith(lowerCaseSite);

            logger.debug("Checking site: {}, Result: {}", lowerCaseSite, isAllowed);

            if (isAllowed) {
                logger.debug("URL: {}, Is Allowed: true", url);
                return true;
            }
        }

        logger.debug("URL: {}, Is Allowed: false");
        logger.info("Denied site: {}", url);
        return false;
    }

    private boolean checkDetailedConditions(String url, String allowedSite) {
        boolean containsAllowedString = url.contains("allowed");

        return containsAllowedString;
    }

    private boolean yourAsyncIndexingLogic(URL pageUrl) {
        try {
            CompletableFuture<Boolean> asyncResult = CompletableFuture.supplyAsync(() -> performAsyncIndexing(pageUrl));

            boolean indexingResult = asyncResult.get();

            return indexingResult;
        } catch (Exception e) {
            logger.error("Error in yourAsyncIndexingLogic for page URL: " + pageUrl, e);
            return false;
        }
    }

    private boolean performAsyncIndexing(URL pageUrl) {
        try {
            return true;
        } catch (Exception e) {
            logger.error("Error in performAsyncIndexing for page URL: " + pageUrl, e);
            return false;
        }
    }

    @Override
    @Transactional
    public List<SearchResult> search(String query, String site, int offset, int limit) {
        List<String> queryLemmas = TextAnalyzer.extractLemmas(query);

        List<String> filteredLemmas = filterFrequentLemmas(queryLemmas);

        filteredLemmas.sort(Comparator.comparingInt(this::getLemmaFrequency));

        List<Page> matchingPages = findMatchingPages(filteredLemmas, site, offset, limit);

        List<SearchResult> searchResults = calculateRelevanceAndSort(matchingPages, query);

        return searchResults;
    }

    private List<String> filterFrequentLemmas(List<String> lemmas) {
        double threshold = 0.9;
        int totalPageCount = getTotalPageCount();
        int maxAllowedFrequency = (int) (totalPageCount * threshold);

        return lemmas.stream()
                .filter(lemma -> getLemmaFrequency(lemma) <= maxAllowedFrequency)
                .collect(Collectors.toList());
    }

    private int getTotalPageCount() {
        return 1000;
    }

    @Override
    public <T> List<Page> findMatchingPages(List<T> lemmas, String site, int offset, int limit) {
        List<Index> indices;

        List<Lemma> lemmaEntities = lemmas.stream()
                .map(lemma -> lemmaRepository.findByLemma((String) lemma).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (StringUtils.isEmpty(site)) {
            indices = indexRepository.findByLemmaIn(lemmaEntities);
        } else {
            indices = indexRepository.findByLemmaInAndPageSiteUrl(lemmaEntities, site);
        }

        List<Page> pages = indices.stream()
                .map(Index::getPage)
                .distinct()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
        return pages;
    }


    @Override
    public List<Index> findByLemmaInAndPageSiteUrl(List<Lemma> lemmas, String siteUrl) {
        return indexRepository.findByLemmaInAndPageSiteUrl(lemmas, siteUrl);
    }


    private int getLemmaFrequency(String lemma) {
        Optional<Lemma> optionalLemma = lemmaRepository.findByLemma(lemma);

        return optionalLemma.map(Lemma::getFrequency).orElse(0);
    }

    private List<SearchResult> calculateRelevanceAndSort(List<Page> pages, String query) {
        List<SearchResult> searchResults = new ArrayList<>();

        for (Page page : pages) {
            String title = "";
            String content = "";

            float relevance = calculateRelevance(query, title, content);

            SearchResult searchResult = new SearchResult();
            searchResult.setPage(page);
            searchResult.setRelevance(relevance);

            searchResults.add(searchResult);
        }

        searchResults.sort(Comparator.comparing(SearchResult::getRelevance).reversed());

        return searchResults;
    }


    private float calculateRelevance(String query, String title, String content) {
        int titleMatches = StringUtils.countOccurrencesOf(title.toLowerCase(), query.toLowerCase());
        int contentMatches = StringUtils.countOccurrencesOf(content.toLowerCase(), query.toLowerCase());
        int queryLength = query.length();

        float relevance = (titleMatches + contentMatches) / (float) queryLength;

        return relevance;
    }

    private boolean myIndexingLogic(URL pageUrl) {
        try {
            IndexingTaskResult result = performIndexingLogic(pageUrl);

            if (result != null && !result.getPageUrls().isEmpty()) {
                Site site = siteRepository.findByUrlIgnoreCase(pageUrl.toString());

                if (site == null) {
                    site = new Site();
                    site.setUrl(pageUrl.toString());

                    Site savedSite = siteRepository.save(site);
                    logger.info("Site saved: {}", savedSite);

                    if (savedSite != null && savedSite.getId() != null) {
                        logger.info("New site saved to the database with URL: {}", savedSite.getUrl());
                    } else {
                        logger.error("Failed to save the new site to the database with URL: {}", pageUrl);
                        return false;
                    }
                } else {
                    logger.info("Site found in the database with URL: {}", site.getUrl());
                }

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("Error in myIndexingLogic for page URL: " + pageUrl, e);
            return false;
        }
    }

    private IndexingTaskResult performIndexingLogic(URL inputPageUrl) {
        IndexingTaskResult result = new IndexingTaskResult();
        String pageUrl = "https://skillbox.ru";
        result.getPageUrls().add(pageUrl);
        return result;
    }

    @Override
    @Transactional
    public List<URL> crawlPages(String siteUrl) {
        try {
            URL initialUrl = new URL(siteUrl);

            IndexingRecursiveTask indexingTask = new IndexingRecursiveTask(initialUrl, visitedUrls, stopIndexingFlag, 0);
            IndexingTaskResult result = forkJoinPool.invoke(indexingTask);

            visitedUrls.addAll(result.getPageUrls().stream().map(this::toURL).collect(Collectors.toSet()));

            return result.getPageUrls().stream().map(this::toURL).collect(Collectors.toList());
        } catch (MalformedURLException e) {
            logger.error("Malformed URL: {}", siteUrl, e);
            throw new RuntimeException("Malformed URL: " + siteUrl, e);
        } catch (Exception e) {
            logger.error("Error while crawling pages", e);
            throw new RuntimeException("Error while crawling pages", e);
        }
    }

    private URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error converting String to URL", e);
        }
    }

    private String getPageContent(URL url) {
        return "";
    }

    private List<String> extractLemmas(String query) {
        return TextAnalyzer.extractLemmas(query);
    }

    public static class IndexingStoppedException extends RuntimeException {
        public IndexingStoppedException(String message) {
            super(message);
        }
    }

}
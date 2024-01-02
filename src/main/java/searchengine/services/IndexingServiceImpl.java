package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingRecursiveTask;
import searchengine.dto.indexing.IndexingTaskResult;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.TextAnalyzer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;



import static searchengine.model.SiteStatus.INDEXING;

@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);
    private static final int MAX_FREQUENCY = 100; //

    private final Environment environment;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final Set<URL> visitedUrls = new HashSet<>();
    private volatile boolean stopIndexing = false;

    @Autowired
    public IndexingServiceImpl(Environment environment, SiteRepository siteRepository,
                               PageRepository pageRepository, LemmaRepository lemmaRepository,
                               IndexRepository indexRepository) {
        this.environment = environment;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    @Transactional
    public void stopIndexing() {
        stopIndexing = true;
    }

    @Override
    @Transactional
    public void startIndexing() {
        logger.info("startIndexing method is called");

        try {
            List<Map<String, String>> siteConfigs = Optional
                    .ofNullable(environment.getProperty("indexing-settings.sites", List.class))
                    .orElse(Collections.emptyList());

            List<Site> sites = siteConfigs.stream()
                    .map(siteConfig -> {
                        Site site = new Site();
                        site.setStatus(INDEXING);
                        site.setStatusTime(LocalDateTime.now());
                        site.setName(siteConfig.get("name"));
                        site.setUrl(siteConfig.get("url"));
                        site.initializeName("NAME");
                        siteRepository.save(site);
                        logger.info("Site saved: {}", site);
                        return site;
                    })
                    .collect(Collectors.toList());

            logger.info("Found {} sites for indexing", sites.size());
            logger.debug("Sites to be indexed: {}", sites);

            ForkJoinPool forkJoinPool = new ForkJoinPool();

            List<CompletableFuture<IndexingTaskResult>> futures = sites.stream()
                    .map(site -> CompletableFuture.supplyAsync(() -> {
                        try {
                            if (stopIndexing) {
                                throw new IndexingStoppedException("Индексация остановлена пользователем");
                            }
                            return forkJoinPool.invoke(new IndexingRecursiveTask(new URL(site.getUrl()), visitedUrls));
                        } catch (MalformedURLException e) {
                            logger.error("Malformed URL for site: {}", site.getUrl(), e);
                            return null;
                        }
                    }))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            allOf.join();

            for (int i = 0; i < futures.size(); i++) {
                Site site = sites.get(i);
                CompletableFuture<IndexingTaskResult> future = futures.get(i);

                try {
                    IndexingTaskResult taskResult = future.get();
                    logger.info("Task result for site {}: {}", site.getUrl(), taskResult);
                    if (taskResult != null) {
                        processIndexingResult(site, taskResult);
                    } else {
                        logger.error("Error processing indexing result for site: {}. Task result is null.", site.getUrl());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Error while getting task result for site: {}", site.getUrl(), e);
                }
            }

            logger.debug("Indexing process completed successfully");
        } catch (IndexingStoppedException e) {
            logger.info("Indexing stopped by the user");
        } catch (Exception e) {
            logger.error("Unexpected error during indexing process", e);
        }
    }

    @Transactional
    public void processIndexingResult(Site site, IndexingTaskResult result) {
        logger.info("Processing indexing result for site: {}", site.getUrl());

        for (String pageUrl : result.getPageUrls()) {
            try {
                URL url = new URL(pageUrl);

                // Добавим проверку на ошибочные HTTP-ответы
                if (!isValidHttpResponse(url)) {
                    logger.warn("Skipping indexing for {} due to error HTTP response", pageUrl);
                    continue;
                }

                String pageContent = getPageContent(url);
                List<String> lemmas = extractLemmas(pageContent);

                for (String lemmaText : lemmas) {
                    saveLemmaToDatabase(site, lemmaText);
                    logger.debug("Lemma saved for site: {}, text: {}", site.getUrl(), lemmaText);
                }

                Page page = new Page();
                page.setSite(site);
                page.setPath(url.getPath());
                pageRepository.save(page);
                logger.debug("Page saved for site: {}", site.getUrl());

                for (String lemmaText : lemmas) {
                    Optional<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaText);
                    if (optionalLemma.isPresent()) {
                        Lemma lemma = optionalLemma.get();

                        Index index = new Index();
                        index.setPage(page);
                        index.setLemma(lemma);
                        index.setRankValue(0.8f);
                        indexRepository.save(index);
                        logger.info("index saved: " + index);
                        logger.debug("Index saved for site: {}, lemma: {}", site.getUrl(), lemmaText);
                    } else {
                        logger.error("Lemma not found for site {} and lemma {}", site.getUrl(), lemmaText);
                    }
                }

                logger.info("Page added for site: {}", site.getUrl());
            } catch (MalformedURLException e) {
                logger.error("Malformed URL while processing page for site {}: {}", site.getUrl(), pageUrl, e);
            } catch (Exception e) {
                logger.error("Error processing page for site {}: {}", site.getUrl(), pageUrl, e);
            }

        }

        site.setStatus(SiteStatus.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        site.initializeName("NAME");
        siteRepository.save(site);
        logger.debug("Site saved: {}", site);

        logger.info("Processing indexing result completed for site: {}", site.getUrl());
    }

    private boolean isValidHttpResponse(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            int responseCode = connection.getResponseCode();

            // Проверяем, является ли код ответа успешным (2xx) или в диапазоне ошибок (4xx и 5xx)
            return (responseCode >= 200 && responseCode < 300);
        } catch (IOException e) {
            logger.error("Error checking HTTP response for URL: {}", url, e);  // Используем параметризованный метод логирования
            return false;
        }
    }

    private void saveLemmaToDatabase(Site site, String lemmaText) {
        try {
            Optional<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaText);

            if (optionalLemma.isPresent()) {
                // Если лемма уже существует в базе данных, увеличиваем частоту
                Lemma existingLemma = optionalLemma.get();
                existingLemma.setFrequency(existingLemma.getFrequency() + 1);
                lemmaRepository.save(existingLemma);
                logger.info("Existing lemma saved: {}", existingLemma);
            } else {
                // Если леммы нет в базе данных, создаем новую запись
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

            // Если сайт не найден в базе данных, считаем его разрешенным для индексации
            boolean isSiteAllowed = site != null || isSiteAllowed(pageUrl.toString());

            if (!isSiteAllowed) {
                // Если переданная страница не принадлежит разрешенным сайтам, выдаем ошибку
                throw new IllegalArgumentException("Индексация страницы с других сайтов запрещена");
            }

            if (site == null) {
                site = new Site();
                site.setUrl(pageUrl.toString());
                site.setStatus(INDEXING);
                site.setStatusTime(LocalDateTime.now());
                site.initializeName("NAME");
                siteRepository.save(site);
                logger.info("Site saved: " + site);

            }

            boolean indexingResult = yourIndexingLogic(pageUrl);

            // Обновим статус и время в зависимости от результата индексации
            site.setStatus(indexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.initializeName("NAME");
            siteRepository.save(site);
            logger.info("Site saved: " + site);


            // Асинхронно обрабатываем индексацию, чтобы не блокировать основной поток
            Site finalSite = site;
            CompletableFuture.runAsync(() -> {
                try {
                    boolean asyncIndexingResult = yourAsyncIndexingLogic(pageUrl);
                    // Обновим статус и время в зависимости от результата асинхронной индексации
                    finalSite.setStatus(asyncIndexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED);
                    finalSite.setStatusTime(LocalDateTime.now());
                    finalSite.initializeName("NAME");
                    siteRepository.save(finalSite);
                    logger.info("Site saved: " + finalSite);

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

    private boolean isSiteAllowed(String url) {
        List<String> allowedSites = Optional.ofNullable(environment.getProperty("indexing-settings.sites", List.class))
                .orElse(Collections.emptyList());
        return allowedSites.stream().anyMatch(siteUrl -> url.startsWith(siteUrl));
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
        // Разбиваем поисковый запрос на леммы
        List<String> queryLemmas = TextAnalyzer.extractLemmas(query);

        // Исключаем леммы, которые встречаются на слишком большом количестве страниц
        List<String> filteredLemmas = filterFrequentLemmas(queryLemmas);

        // Сортируем леммы в порядке увеличения частоты встречаемости
        filteredLemmas.sort(Comparator.comparingInt(this::getLemmaFrequency));

        // Ищем страницы, соответствующие запросу
        List<Page> matchingPages = findMatchingPages(filteredLemmas, site, offset, limit);

        // Рассчитываем релевантность и сортируем страницы по убыванию релевантности
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

        if (StringUtils.isEmpty(site)) {
            // Если сайт не указан, ищем по всем сайтам
            indices = indexRepository.findByLemmaIn((List<Lemma>) lemmas);
        } else {
            // Если указан конкретный сайт, ищем по нему
            indices = indexRepository.findByLemmaInAndPageSiteUrl((List<Lemma>) lemmas, site);
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
        return 0;
    }

    private List<SearchResult> calculateRelevanceAndSort(List<Page> pages, String query) {
        return Collections.emptyList();
    }

    private float calculateRelevance(String query, String title, String content) {
        return 0.75f;
    }


    private boolean yourIndexingLogic(URL pageUrl) {
        try {
            IndexingTaskResult result = new IndexingTaskResult();

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
            logger.error("Error in yourIndexingLogic for page URL: " + pageUrl, e);
            return false;
        }
    }

    @Override
    @Transactional
    public List<URL> crawlPages(String siteUrl) {
        try {
            URL initialUrl = new URL(siteUrl);
            List<URL> initialPages = List.of(initialUrl);

            IndexingRecursiveTask indexingTask = new IndexingRecursiveTask(initialUrl, visitedUrls);
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

    private void deleteSiteData(Long siteId) {
        pageRepository.deleteBySiteId(siteId);
    }

    private String getPageContent(URL url) {
        return "";
    }

    private List<String> extractLemmas(String query) {

        return Arrays.asList(query.split("\\s+"));
    }

    private void sortByFrequency(List<String> lemmas) {

    }

    private void handleIndexingError(Site site, Exception e) {
        logger.error("Error processing indexing result for site: {}", site.getUrl(), e);

        site.setStatus(SiteStatus.FAILED);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(e.getMessage());
        site.initializeName("NAME");
        siteRepository.save(site);
        logger.debug("Site saved: {}", site);
    }

    public static class IndexingStoppedException extends RuntimeException {
        public IndexingStoppedException(String message) {
            super(message);
        }

        public IndexingStoppedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}

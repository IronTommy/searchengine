package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingRecursiveTask;
import searchengine.dto.indexing.IndexingTaskResult;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final Set<URL> visitedUrls = new HashSet<>();
    private volatile boolean stopIndexing = false;

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
            logger.debug("startIndexing method is called");

            List<Site> sites = siteRepository.findAll();
            logger.info("Found {} sites for indexing", sites.size());

            // Добавим новую строку для логирования
            logger.info("Start indexing process for sites");

            List<Callable<IndexingTaskResult>> tasks = sites.stream()
                    .map(site -> (Callable<IndexingTaskResult>) () -> {
                        if (stopIndexing) {
                            throw new IndexingStoppedException("Индексация остановлена пользователем");
                        }
                        return forkJoinPool.invoke(
                                new IndexingRecursiveTask(
                                        new URL(site.getUrl()), visitedUrls
                                ));
                    })
                    .collect(Collectors.toList());

            logger.info("Submitting tasks for indexing");

            List<Future<IndexingTaskResult>> results = forkJoinPool.invokeAll(tasks);

            for (int i = 0; i < results.size(); i++) {
                Site site = sites.get(i);
                Future<IndexingTaskResult> result = results.get(i);

                try {
                    IndexingTaskResult taskResult = result.get();
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
        } finally {
            try {
                forkJoinPool.shutdown();
                forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error while shutting down ForkJoinPool", e);
            }
        }
    }

    @Transactional
    public void processIndexingResult(Site site, IndexingTaskResult result) {
        logger.info("Processing indexing result for site: {}", site.getUrl());

        try {
            logger.debug("Inside processIndexingResult method. Checking if indexing is progressing.");

            deleteSiteData(site.getId());

            site.setStatus(SiteStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            Set<String> pagesSet = result.getPageUrls();
            List<String> pages = new ArrayList<>(pagesSet);
            for (String pageUrl : pages) {
                try {
                    URL url = new URL(pageUrl);
                    String pageContent = getPageContent(url); // Ваш метод для получения текста страницы
                    List<String> lemmas = extractLemmas(pageContent); // Ваш метод для извлечения лемм

                    for (String lemmaText : lemmas) {
                        Lemma lemma = new Lemma();
                        lemma.setSite(site);
                        lemma.setLemma(lemmaText);
                        lemma.setFrequency(1);  // Ваш код для извлечения частоты
                        lemmaRepository.save(lemma);
                    }

                    Page page = new Page();
                    page.setSite(site);
                    page.setPath(url.getPath());
                    pageRepository.save(page);

                    for (String lemmaText : lemmas) {
                        Optional<Lemma> optionalLemma = lemmaRepository.findBySiteAndLemma(site, lemmaText);
                        if (optionalLemma.isPresent()) {
                            Lemma lemma = optionalLemma.get();

                            Index index = new Index();
                            index.setPage(page);
                            index.setLemma(lemma);
                            index.setRankValue(0.8f);  // Ваш код для определения ранга
                            indexRepository.save(index);
                        } else {
                            // Обработка случая, если лемма не найдена
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
            siteRepository.save(site);

            logger.info("Processing indexing result completed for site: {}", site.getUrl());

        } catch (Exception e) {
            logger.error("Error processing indexing result for site: {}", site.getUrl(), e);

            site.setStatus(SiteStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(e.getMessage());
            siteRepository.save(site);
        }
    }


    @Override
    @Transactional
    public boolean isIndexingInProgress() {
        // Ваш код для проверки, выполняется ли индексация
        return false;
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
            // Логирование ошибки
            logger.error("Malformed URL: {}", siteUrl, e);
            // Можно выбросить исключение или выполнить другие действия в зависимости от логики приложения
            throw new RuntimeException("Malformed URL: " + siteUrl, e);
        } catch (Exception e) {
            // Логирование ошибки
            logger.error("Error while crawling pages", e);
            // Можно выбросить исключение или выполнить другие действия в зависимости от логики приложения
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
        // Ваш код для получения текста страницы по URL
        return ""; // Замените на ваш реальный код
    }

    private List<String> extractLemmas(String pageContent) {
        // Ваш код для извлечения лемм из текста страницы
        // Пример: простая реализация, разделяющая текст на слова
        String[] words = pageContent.split("\\s+");
        return Arrays.asList(words);
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

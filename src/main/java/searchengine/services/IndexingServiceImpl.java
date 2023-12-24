package searchengine.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingRecursiveTask;
import searchengine.dto.indexing.IndexingTaskResult;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.DelayUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final Set<URL> visitedUrls = new HashSet<>();

    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }


    @Override
    public void startIndexing() {
        List<Site> sites = siteRepository.findAll();

        List<Callable<IndexingTaskResult>> tasks = sites.stream()
                .map(site -> (Callable<IndexingTaskResult>) ()
                        -> {
                    // Добавляем задержку перед выполнением запроса
                    DelayUtils.delayBetweenRequests();
                    return forkJoinPool.invoke(
                            new IndexingRecursiveTask(
                                    new URL(site.getUrl()),  // Преобразовать строку в URL
                                    visitedUrls
                            ));
                })
                .collect(Collectors.toList());

        List<Future<IndexingTaskResult>> results;
        results = forkJoinPool.invokeAll(tasks);

        for (int i = 0; i < sites.size(); i++) {
            Site site = sites.get(i);
            Future<IndexingTaskResult> result = results.get(i);

            try {
                IndexingTaskResult taskResult = result.get();
                if (taskResult != null) {
                    processIndexingResult(site, taskResult);
                } else {
                    // Обработка ошибки, если что-то пошло не так
                }
                // лучше добавить логирование ошибок
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error while getting task result", e);
            }
        }
    }


    @Transactional
    public void processIndexingResult(Site site, IndexingTaskResult result) {
        try {
            // Удаляем имеющиеся данные по сайту
            deleteSiteData(site.getId());

            // Создаем новую запись в таблице site со статусом INDEXING
            site.setStatus(SiteStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            // Обходим страницы и добавляем их в базу данных
            Set<String> pagesSet = result.getPageUrls();
            List<String> pages = new ArrayList<>(pagesSet);
            for (String pageUrl : pages) {
                // Преобразуем String в URL
                URL url = new URL(pageUrl);

                Page page = new Page();
                page.setSite(site);
                page.setPath(url.getPath());
                // Другие поля page заполняются в зависимости от вашей логики
                pageRepository.save(page);
            }

            // Обновляем статус в таблице site на INDEXED
            site.setStatus(SiteStatus.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

        } catch (Exception e) {
            // В случае ошибки обновляем статус на FAILED и сохраняем сообщение об ошибке
            site.setStatus(SiteStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(e.getMessage());
            siteRepository.save(site);
        }
    }


    @Override
    public boolean isIndexingInProgress() {
        // Ваш код для проверки, выполняется ли индексация
        return false;
    }

    private void deleteSiteData(Long siteId) {
        // Удаление данных по сайту из таблицы page
        pageRepository.deleteBySiteId(siteId);

        // Другие операции по удалению данных (если нужны)
    }

    @Override
    public List<URL> crawlPages(String siteUrl) {
        try {
            URL initialUrl = new URL(siteUrl);
            List<URL> initialPages = List.of(initialUrl);

            // Создаем ForkJoinTask
            IndexingRecursiveTask indexingTask = new IndexingRecursiveTask(initialUrl, visitedUrls);

            // Запускаем ForkJoinTask в ForkJoinPool
            IndexingTaskResult result = forkJoinPool.invoke(indexingTask);

            // Обновляем посещенные ссылки
            visitedUrls.addAll(result.getPageUrls().stream().map(this::toURL).collect(Collectors.toSet()));

            // Возвращаем список URL страниц
            return result.getPageUrls().stream().map(this::toURL).collect(Collectors.toList());
        } catch (Exception e) {
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
}

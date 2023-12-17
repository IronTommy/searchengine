package searchengine.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexing.IndexingRecursiveTask;
import searchengine.dto.indexing.IndexingTaskResult;
import searchengine.model.Site;
import searchengine.model.Page;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public IndexingServiceImpl(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    @Transactional
    public void startIndexing() {
        List<Site> sites = siteRepository.findAll();

        for (Site site : sites) {
            try {
                // Удаляем имеющиеся данные по сайту
                deleteSiteData(site.getId());

                // Создаем новую запись в таблице site со статусом INDEXING
                site.setStatus(SiteStatus.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                // Обходим страницы и добавляем их в базу данных
                // (здесь предполагается, что у вас есть метод для обхода страниц)
                List<String> pages = crawlPages(site.getUrl());
                for (String pageUrl : pages) {
                    Page page = new Page();
                    page.setSite(site);
                    page.setPath(pageUrl);
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
    public List<String> crawlPages(String siteUrl) {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        List<String> initialPages = List.of(siteUrl);

        // Создаем ForkJoinTask
        IndexingRecursiveTask indexingTask = new IndexingRecursiveTask(siteUrl);

        // Запускаем ForkJoinTask в ForkJoinPool
        IndexingTaskResult result = forkJoinPool.invoke(indexingTask);

        // Возвращаем список URL страниц
        return result.getPageUrls();
    }
}

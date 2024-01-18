package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Site;

import java.util.Collections;
import java.util.List;

public class SiteCrawlingTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawlingTask.class);

    private final Site site;
    private final IndexingService indexingService;

    public SiteCrawlingTask(Site site, IndexingService indexingService) {
        this.site = site;
        this.indexingService = indexingService;
    }

    @Override
    public void run() {
        /*try {
            logger.info("Calling startIndexing from SiteCrawlingTask for site: {}", site.getUrl());
            List<Site> singleSiteList = Collections.singletonList(site);
            indexingService.startIndexing(singleSiteList);
        } catch (Exception e) {
            // Логирование ошибки
            logger.error("Error during site crawling task for site: {}", site.getUrl(), e);
        }*/
    }
}

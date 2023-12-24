package searchengine.services;

import searchengine.model.Site;

public class SiteCrawlingTask implements Runnable {

    private final Site site;
    private final IndexingService indexingService;

    public SiteCrawlingTask(Site site, IndexingService indexingService) {
        this.site = site;
        this.indexingService = indexingService;
    }

    @Override
    public void run() {
        try {
            indexingService.startIndexing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

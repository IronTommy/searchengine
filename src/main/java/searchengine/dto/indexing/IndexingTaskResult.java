package searchengine.dto.indexing;

import java.util.List;

public class IndexingTaskResult {
    private final List<String> pageUrls;

    public IndexingTaskResult(List<String> pageUrls) {
        this.pageUrls = pageUrls;
    }

    public List<String> getPageUrls() {
        return pageUrls;
    }
}

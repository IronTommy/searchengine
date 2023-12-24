package searchengine.dto.indexing;

import java.util.HashSet;
import java.util.Set;

public class IndexingTaskResult {
    private final Set<String> pageUrls;

    // Конструктор по умолчанию
    public IndexingTaskResult() {
        this.pageUrls = new HashSet<>();
    }

    // Конструктор с аргументами
    public IndexingTaskResult(Set<String> pageUrls) {
        this.pageUrls = pageUrls;
    }

    public Set<String> getPageUrls() {
        return pageUrls;
    }
}

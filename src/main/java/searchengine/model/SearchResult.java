package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class SearchResult {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
    private Page page;

    public SearchResult() {

    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }
}

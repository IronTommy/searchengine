package searchengine.dto.indexing;

import java.util.List;
import java.util.concurrent.RecursiveTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class IndexingTask extends RecursiveTask<List<String>> {

    private final String url;

    public IndexingTask(String url) {
        this.url = url;
    }

    @Override
    protected List<String> compute() {
        try {
            Document document = Jsoup.connect(url).get();
            Elements links = document.select("a[href]");
            return links.eachAttr("abs:href");
        } catch (Exception e) {
            throw new RuntimeException("Error while processing IndexingTask", e);
        }
    }
}

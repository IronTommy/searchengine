package searchengine.dto.indexing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GlobalLinkStorage {
    private static final List<String> globalLinksList = new ArrayList<>();

    public static synchronized void addLinks(Collection<String> links) {
        globalLinksList.addAll(links);
    }

    public static synchronized List<String> getGlobalLinks() {
        return new ArrayList<>(globalLinksList);
    }
}

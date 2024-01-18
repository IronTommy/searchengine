package searchengine.dto.indexing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Класс для хранения общего списка ссылок
public class GlobalLinkStorage {
    // Статический общий список ссылок
    private static final List<String> globalLinksList = new ArrayList<>();

    // Метод для добавления ссылок в общий список (синхронизированный, чтобы избежать проблем с параллелизмом)
    public static synchronized void addLinks(Collection<String> links) {
        globalLinksList.addAll(links);
    }

    // Метод для получения копии общего списка (синхронизированный)
    public static synchronized List<String> getGlobalLinks() {
        return new ArrayList<>(globalLinksList);
    }
}

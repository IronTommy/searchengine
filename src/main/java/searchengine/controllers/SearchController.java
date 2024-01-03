package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.model.Page;
import searchengine.services.IndexingService;
import searchengine.utils.TextAnalyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final IndexingService indexingService;

    @Autowired
    public SearchController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        if (StringUtils.isEmpty(query)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", "Задан пустой поисковый запрос");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Разбиваем поисковый запрос на леммы
            List<String> queryLemmas = TextAnalyzer.extractLemmas(query);
            System.out.println("Extracted Lemmas: " + queryLemmas);

            // Исключаем леммы, которые встречаются на слишком большом количестве страниц
            List<String> filteredLemmas = filterFrequentLemmas(queryLemmas);

            // Получаем результаты поиска из индекса
            List<Page> searchResults = indexingService.findMatchingPages(filteredLemmas, site, offset, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("count", searchResults.size());
            response.put("data", searchResults);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // для вывода подробной информации об ошибке
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("result", false);
            errorResponse.put("error", "Внутренняя ошибка сервера");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    // Метод для фильтрации лемм, которые встречаются на слишком большом количестве страниц
    private List<String> filterFrequentLemmas(List<String> lemmas) {
        // Например, исключение лемм, которые встречаются на более чем 90% страниц
        double threshold = 0.9;
        int totalPageCount = getTotalPageCount();
        int maxAllowedFrequency = (int) (totalPageCount * threshold);

        return lemmas.stream()
                .filter(lemma -> getLemmaFrequency(lemma) <= maxAllowedFrequency)
                .collect(Collectors.toList());
    }

    private int getTotalPageCount() {
        return 1000;
    }

    private int getLemmaFrequency(String lemma) {
        return 50;
    }
}

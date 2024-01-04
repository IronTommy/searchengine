package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.model.SearchResult;
import searchengine.services.IndexingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final IndexingService indexingService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {

        if (StringUtils.isEmpty(query)) {
            return ResponseEntity.badRequest().body(Map.of("result", false, "error", "Задан пустой поисковый запрос"));
        }

        try {
            List<SearchResult> searchResults = indexingService.search(query, site, offset, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("count", searchResults.size());
            response.put("data", searchResults);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("result", false, "error", "Внутренняя ошибка сервера"));
        }
    }
}

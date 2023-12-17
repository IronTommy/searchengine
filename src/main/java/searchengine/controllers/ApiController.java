package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = new IndexingResponse();

        // Проверяем, не запущена ли уже индексация
        if (indexingService.isIndexingInProgress()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return ResponseEntity.badRequest().body(response);
        }

        // Запускаем индексацию
        try {
            indexingService.startIndexing();
            response.setResult(true);
        } catch (Exception e) {
            response.setResult(false);
            response.setError("Произошла ошибка при запуске индексации");
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }
}

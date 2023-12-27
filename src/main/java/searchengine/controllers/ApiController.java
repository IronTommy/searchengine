package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.StatisticsService;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    @Transactional
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = new IndexingResponse();

        try {
            logger.info("Calling startIndexing from ApiController");
            indexingService.startIndexing();
            response.setResult(true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            errorLogger.error("Error during indexing", e);
            response.setResult(false);
            response.setError("Произошла ошибка при запуске индексации");
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        try {
            indexingService.stopIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (IndexingServiceImpl.IndexingStoppedException e) {
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("result", false, "error", "Ошибка остановки индексации"));
        }
    }
}


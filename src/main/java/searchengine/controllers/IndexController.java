package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.services.IndexingService;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IndexController {

    private final IndexingService indexingService;

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody Map<String, String> requestBody) {
        try {
            String urlString = requestBody.get("pageUrl");
            URL pageUrl = new URL(urlString);

            boolean indexingResult = indexingService.indexPage(pageUrl);
            return ResponseEntity.ok(new IndexingResponse(indexingResult));
        } catch (Exception e) {
            return handleIndexingError(e);
        }
    }

    private ResponseEntity<IndexingResponse> handleIndexingError(Exception e) {
        IndexingResponse response = new IndexingResponse();
        response.setResult(false);
        response.setError("Произошла ошибка во время индексации. Подробности в логах.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

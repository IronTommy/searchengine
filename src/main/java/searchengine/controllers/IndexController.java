package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private SiteRepository siteRepository;


    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody Map<String, String> requestBody) {
        try {
            // Валидация URL
            String urlString = requestBody.get("pageUrl");
            // Добавим лог перед созданием объекта URL
            logger.info("Попытка создания объекта URL для строки: {}", urlString);

            URL pageUrl = new URL(urlString);

            /*// Создание объекта Site
            Site site = new Site();
            site.setUrl("https://www.skillbox.ru");
            site.setName("Skillbox"); */

            // Запуск логики индексации отдельной страницы
            boolean indexingResult = indexingService.indexPage(pageUrl);

            // Возвращаем результат индексации
            return ResponseEntity.ok(new IndexingResponse(indexingResult));
        } catch (Exception e) {
            // Обработка ошибок валидации URL или других исключений
            logger.error("Ошибка во время индексации", e);
            String errorMessage = "Произошла ошибка во время индексации. Подробности в логах.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new IndexingResponse(false, errorMessage));
        }
    }



}

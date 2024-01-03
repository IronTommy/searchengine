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
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;

import java.net.URL;
import java.time.LocalDateTime;
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

            // Проверка существования сайта в базе данных
            Site site = siteRepository.findByUrlIgnoreCase(urlString);
            if (site == null) {
                // Создание объекта Site, если его еще нет в базе данных
                site = new Site();
                site.setUrl(urlString);
                site.setName("Name");
                site.setStatus(SiteStatus.INDEXING); // Установите статус здесь
                site.setStatusTime(LocalDateTime.now()); // Установите текущее время
                siteRepository.save(site);
            }

            // Обновление статуса и других данных сайта перед индексацией
            site.setStatus(SiteStatus.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(null); // Сбрасываем последнюю ошибку

            // Сохранение обновленных данных сайта в базе данных
            siteRepository.save(site);

            // Запуск логики индексации отдельной страницы
            boolean indexingResult = indexingService.indexPage(pageUrl);

            // Обновление статуса и времени после индексации
            if (indexingResult) {
                site.setStatus(SiteStatus.INDEXED);
            } else {
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Произошла ошибка во время индексации. Подробности в логах.");
            }
            site.setStatusTime(LocalDateTime.now());

            // Сохранение обновленных данных сайта в базе данных
            siteRepository.save(site);

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

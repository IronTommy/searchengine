package searchengine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final SiteRepository siteRepository;

    public DataLoader(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public void run(String... args) {
        // Загрузка данных при старте приложения, если таблица пуста
        List<Site> existingSites = siteRepository.findAll();
        if (existingSites.isEmpty()) {
            initializeSites();
        }
    }

    private void initializeSites() {
    }
}

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
        // Пример: добавление начальных данных
        Site site1 = new Site();
        site1.setName("Лента.ру");
        site1.setUrl("https://www.lenta.ru");
        site1.setStatus(SiteStatus.INDEXING);
        site1.setStatusTime(LocalDateTime.now());
        siteRepository.save(site1);

        Site site2 = new Site();
        site2.setName("Skillbox");
        site2.setUrl("https://www.skillbox.ru");
        site2.setStatus(SiteStatus.INDEXING);
        site2.setStatusTime(LocalDateTime.now());
        siteRepository.save(site2);

        Site site3 = new Site();
        site3.setName("PlayBack.Ru");
        site3.setUrl("https://www.playback.ru");
        site3.setStatus(SiteStatus.INDEXING);
        site3.setStatusTime(LocalDateTime.now());
        siteRepository.save(site3);

        Site site4 = new Site();
        site4.setName("VolochekLife");
        site4.setUrl("https://volochek.life");
        site4.setStatus(SiteStatus.INDEXING);
        site4.setStatusTime(LocalDateTime.now());
        siteRepository.save(site4);

    }
}

package searchengine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final SiteRepository siteRepository;

    public DataLoader(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public void run(String... args) {
        List<Site> existingSites = siteRepository.findAll();
        if (existingSites.isEmpty()) {
            initializeSites();
        }
    }

    private void initializeSites() {
    }
}

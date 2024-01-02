package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    @Transactional
    void deleteBySiteId(Long siteId);

    Optional<Page> findByUrl(String url);

    boolean existsBySiteAndPath(Site site, String path);
}

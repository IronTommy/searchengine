package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import javax.transaction.Transactional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    @Transactional
    void deleteBySiteId(Long siteId);
}

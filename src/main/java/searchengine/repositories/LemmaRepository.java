package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Optional<Lemma> findBySiteAndLemma(Site site, String lemma);

    Optional<Lemma> findByLemma(String lemmaText);

    List<Lemma> findByLemmaIn(List<String> lemmas);

}

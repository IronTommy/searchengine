package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.LemmatizationExample;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HtmlProcessingService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private LemmatizationExample lemmatizer;

    @Transactional
    public void processHtml(String url, String htmlContent) {
        try {
            Page page = saveOrUpdatePage(url);

            List<String> lemmas = extractLemmasWithLucene(htmlContent);

            updateLemmaTable(lemmas);

            updateIndexTable(page, lemmas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Page saveOrUpdatePage(String url) {
        return pageRepository.findByUrl(url).orElseGet(() ->
                pageRepository.save(new Page(url)));
    }


    private List<String> extractLemmasWithLucene(String htmlContent) throws IOException {
        String[] words = htmlContent.split("\\s+");
        return Arrays.stream(words)
                .flatMap(word -> {
                    try {
                        return lemmatizer.getNormalForms(word).stream();
                    } catch (IOException e) {
                        throw new RuntimeException("Error extracting lemmas", e);
                    }
                })
                .collect(Collectors.toList());
    }

    private void updateLemmaTable(List<String> lemmas) {
        Map<String, Lemma> lemmaMap = new HashMap<>();

        for (String lemmaText : lemmas) {
            lemmaMap.computeIfAbsent(lemmaText, Lemma::new)
                    .incrementFrequency();
        }

        lemmaRepository.saveAll(lemmaMap.values());
    }

    private void updateIndexTable(Page page, List<String> lemmas) {
        List<Index> indices = lemmas.stream()
                .map(lemmaText -> new Index(page, lemmaRepository.findByLemma(lemmaText).orElseThrow()))
                .collect(Collectors.toList());

        indexRepository.saveAll(indices);
    }
}


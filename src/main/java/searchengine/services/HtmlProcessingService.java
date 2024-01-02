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
import java.util.List;
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
        // Получаем или создаем страницу
        Page page = pageRepository.findByUrl(url).orElseGet(() -> {
            Page newPage = new Page();
            newPage.setUrl(url);
            return pageRepository.save(newPage);
        });

        // Извлекаем леммы из HTML-кода с использованием LemmatizationExample
        List<String> lemmas = extractLemmasWithLucene(htmlContent);

        // Обновляем таблицу lemma
        updateLemmaTable(lemmas);

        // Обновляем таблицу index
        updateIndexTable(page, lemmas);
    }

    private List<String> extractLemmasWithLucene(String htmlContent) {
        // Реализуйте ваш код для извлечения лемм из HTML-кода с использованием LemmatizationExample
        String[] words = htmlContent.split("\\s+");
        return Arrays.stream(words)
                .flatMap(word -> {
                    try {
                        return lemmatizer.getNormalForms(word).stream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());
    }

    private void updateLemmaTable(List<String> lemmas) {
        for (String lemmaText : lemmas) {
            Lemma lemma = lemmaRepository.findByLemma(lemmaText).orElseGet(() -> {
                Lemma newLemma = new Lemma();
                newLemma.setLemma(lemmaText);
                newLemma.setFrequency(0);
                return newLemma;
            });

            // Увеличиваем частоту леммы
            lemma.setFrequency(lemma.getFrequency() + 1);

            // Сохраняем или обновляем лемму в таблице
            lemmaRepository.save(lemma);
        }
    }

    private void updateIndexTable(Page page, List<String> lemmas) {
        for (String lemmaText : lemmas) {
            Lemma lemma = lemmaRepository.findByLemma(lemmaText).orElseThrow();

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(1);

            indexRepository.save(index);
        }
    }
}

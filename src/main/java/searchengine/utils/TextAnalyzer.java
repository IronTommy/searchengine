package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.LemmatizationExample;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TextAnalyzer {

    public static final int MAX_FREQUENCY = 100;

    private final LuceneMorphology luceneMorph;

    public TextAnalyzer() throws IOException {
        this.luceneMorph = new RussianLuceneMorphology();
    }

    public static List<String> extractLemmas(String text) {
        return Arrays.stream(text.split("\\s+"))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public String generateSnippet(String content, int maxLength) {
        return content.substring(0, Math.min(content.length(), maxLength));
    }

    public Map<String, Integer> analyzeText(String text) {
        Map<String, Integer> lemmaCountMap = new HashMap<>();

        String[] words = text.split("\\s+");

        for (String word : words) {
            processWord(word, lemmaCountMap);
        }

        return lemmaCountMap;
    }

    private void processWord(String word, Map<String, Integer> lemmaCountMap) {
        if (!word.isEmpty()) {
            String cleanedWord = word.replaceAll("[^\\p{L}]+", "");
            char firstChar = cleanedWord.charAt(0);

            if (Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(firstChar))) {
                String lowercaseWord = cleanedWord.toLowerCase();
                processLemma(lowercaseWord, lemmaCountMap);
            }
        }
    }

    private void processLemma(String lemma, Map<String, Integer> lemmaCountMap) {
        LemmatizationExample lemmatizer = null;
        try {
            lemmatizer = new LemmatizationExample();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            List<String> wordBaseForms = lemmatizer.getNormalForms(lemma);
            wordBaseForms.forEach(baseForm ->
                    lemmaCountMap.put(baseForm, lemmaCountMap.getOrDefault(baseForm, 0) + 1)
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String removeHtmlTags(String htmlText) {
        return htmlText.replaceAll("<[^>]+>", "");
    }

    public static void main(String[] args) {
        try {
            TextAnalyzer textAnalyzer = new TextAnalyzer();

            String inputText = "Повторное появление леопарда в Осетии <b>позволяет</b> предположить, " +
                    "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

            Map<String, Integer> result = textAnalyzer.analyzeText(inputText);

            System.out.println("Результаты анализа текста:");
            result.forEach((lemma, count) -> System.out.println(lemma + " — " + count));

            String htmlText = "<p>Это <b>текст</b> с HTML-тегами.</p>";
            String plainText = textAnalyzer.removeHtmlTags(htmlText);
            System.out.println("Текст без HTML-тегов: " + plainText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.LemmatizationExample;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextAnalyzer {

    private final LuceneMorphology luceneMorph;

    public TextAnalyzer() throws IOException {
        this.luceneMorph = new RussianLuceneMorphology();
    }

    public Map<String, Integer> analyzeText(String text) throws IOException {
        Map<String, Integer> lemmaCountMap = new HashMap<>();

        String[] words = text.split("\\s+");

        for (String word : words) {
            // Проверка, что слово не пустое
            if (!word.isEmpty()) {
                // Удаление знаков препинания
                String cleanedWord = word.replaceAll("[^\\p{L}]+", "");
                // Проверка, что первый символ слова является кириллической буквой
                char firstChar = cleanedWord.charAt(0);
                if (Character.UnicodeBlock.CYRILLIC.equals(Character.UnicodeBlock.of(firstChar))) {
                    // Приведение слова к нижнему регистру и применение лемматизации
                    String lowercaseWord = cleanedWord.toLowerCase();
                    LemmatizationExample lemmatizer = new LemmatizationExample();
                    try {
                        List<String> wordBaseForms = lemmatizer.getNormalForms(lowercaseWord);
                        for (String lemma : wordBaseForms) {
                            lemmaCountMap.put(lemma, lemmaCountMap.getOrDefault(lemma, 0) + 1);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return lemmaCountMap;
    }


    public String removeHtmlTags(String htmlText) {
        // Используйте регулярное выражение для удаления HTML-тегов
        return htmlText.replaceAll("<[^>]+>", "");
    }

    public static void main(String[] args) {
        try {
            TextAnalyzer textAnalyzer = new TextAnalyzer();

            // Пример текста для анализа
            String inputText = "Повторное появление леопарда в Осетии <b>позволяет</b> предположить, " +
                    "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

            // Получение результата анализа
            Map<String, Integer> result = textAnalyzer.analyzeText(inputText);

            // Вывод результатов
            System.out.println("Результаты анализа текста:");
            result.forEach((lemma, count) -> System.out.println(lemma + " — " + count));

            // Пример удаления HTML-тегов
            String htmlText = "<p>Это <b>текст</b> с HTML-тегами.</p>";
            String plainText = textAnalyzer.removeHtmlTags(htmlText);
            System.out.println("Текст без HTML-тегов: " + plainText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.List;

public class LemmatizationExample {

    private final LuceneMorphology luceneMorph;

    public LemmatizationExample() throws IOException {
        this.luceneMorph = new RussianLuceneMorphology();
    }

    public List<String> getNormalForms(String lowercaseWord) throws IOException {
        return luceneMorph.getNormalForms(lowercaseWord);
    }

    public static void main(String[] args) {
        try {
            LemmatizationExample lemmatizer = new LemmatizationExample();

            // Текст для лемматизации
            String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                    "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

            // Разделение текста на слова
            String[] words = text.split("\\s+");

            // Для каждого слова выводим исходную форму
            System.out.println("Результаты лемматизации текста:");
            for (String word : words) {
                List<String> wordBaseForms = lemmatizer.getNormalForms(word);
                System.out.println("Исходные формы слова \"" + word + "\":");
                if (wordBaseForms != null) {
                    wordBaseForms.forEach(System.out::println);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

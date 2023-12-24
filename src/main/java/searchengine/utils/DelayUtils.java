package searchengine.utils;

import java.util.Random;

public class DelayUtils {

    private static final Random random = new Random();

    public static void delayBetweenRequests() {
        try {
            // Генерируем случайное число в диапазоне от 500 мс до 5000 мс (0,5–5 секунд)
            int delay = 500 + random.nextInt(4500);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Обработка исключения, если поток был прерван
            e.printStackTrace();
        }
    }
}

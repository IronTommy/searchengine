package searchengine.dto.indexing;

public class IndexingResponse {
    private boolean result;
    private String error;

    // Конструктор без аргументов
    public IndexingResponse() {
    }

    // Конструктор с рез
    public IndexingResponse(boolean result) {
        this.result = result;
    }

    // Конструктор с аргументами
    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}

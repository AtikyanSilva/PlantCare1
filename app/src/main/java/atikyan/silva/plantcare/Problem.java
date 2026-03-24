package atikyan.silva.plantcare;

public class Problem {
    private String title;
    private String description; // О проблеме
    private String treatment;   // Как бороться
    private int imageResId;

    public Problem(String title, String description, String treatment, int imageResId) {
        this.title = title;
        this.description = description;
        this.treatment = treatment;
        this.imageResId = imageResId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTreatment() { return treatment; }
    public int getImageResId() { return imageResId; }
}
package atikyan.silva.plantcare;

public class ForumMessage {
    private String id;
    private String text;
    private String authorId;
    private String authorName;
    private long   timestamp;

    // Required empty constructor for Firebase deserialization
    public ForumMessage() {}

    public ForumMessage(String text, String authorId, String authorName, long timestamp) {
        this.text       = text;
        this.authorId   = authorId;
        this.authorName = authorName;
        this.timestamp  = timestamp;
    }

    public String getId()         { return id; }
    public String getText()       { return text; }
    public String getAuthorId()   { return authorId; }
    public String getAuthorName() { return authorName; }
    public long   getTimestamp()  { return timestamp; }

    public void setId(String id)             { this.id = id; }
    public void setText(String text)         { this.text = text; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String name)   { this.authorName = name; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

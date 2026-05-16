package atikyan.silva.plantcare;

public class ChatMessage {
    private String text;
    private boolean isBot;
    private boolean isTyping;
    private long timestamp;

    public ChatMessage(String text, boolean isBot) {
        this.text      = text;
        this.isBot     = isBot;
        this.isTyping  = false;
        this.timestamp = System.currentTimeMillis();
    }

    public String  getText()      { return text; }
    public boolean isBot()        { return isBot; }
    public boolean isTyping()     { return isTyping; }
    public long    getTimestamp() { return timestamp; }

    public void setTyping(boolean typing) { this.isTyping = typing; }
}
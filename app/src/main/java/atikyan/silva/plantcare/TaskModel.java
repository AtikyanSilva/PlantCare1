package atikyan.silva.plantcare;

public class TaskModel {
    public String id;
    public String plantName;
    public String taskDescription;
    public String time;
    public String date;           // "yyyy-MM-dd" — дата срабатывания
    public String completedDate;  // "yyyy-MM-dd" — дата выполнения (null = не выполнено)
    public boolean isCompleted;

    public TaskModel() {}

    public TaskModel(String id, String plantName, String taskDescription, String time, String date) {
        this.id              = id;
        this.plantName       = plantName;
        this.taskDescription = taskDescription;
        this.time            = time;
        this.date            = date;
        this.isCompleted     = false;
        this.completedDate   = null;
    }
}

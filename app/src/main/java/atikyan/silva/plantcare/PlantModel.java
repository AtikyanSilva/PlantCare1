package atikyan.silva.plantcare;

public class PlantModel {

    private String  docId;
    private String  name;
    private String  imageUrl;
    private String  photoBase64;   // фото в base64
    private Long    nextWater;
    private Long    waterDays;     // интервал полива в днях
    private String  waterLabel;
    private String  room;
    private String  pot;
    private String  light;         // освещение
    private String  notes;         // заметки

    public PlantModel() {}

    // Getters & Setters
    public String  getDocId()                  { return docId; }
    public void    setDocId(String v)          { this.docId = v; }

    public String  getName()                   { return name; }
    public void    setName(String v)           { this.name = v; }

    public String  getImageUrl()               { return imageUrl; }
    public void    setImageUrl(String v)       { this.imageUrl = v; }

    public String  getPhotoBase64()            { return photoBase64; }
    public void    setPhotoBase64(String v)    { this.photoBase64 = v; }

    public Long    getNextWater()              { return nextWater; }
    public void    setNextWater(Long v)        { this.nextWater = v; }

    public Long    getWaterDays()              { return waterDays; }
    public void    setWaterDays(Long v)        { this.waterDays = v; }

    public String  getWaterLabel()             { return waterLabel; }
    public void    setWaterLabel(String v)     { this.waterLabel = v; }

    public String  getRoom()                   { return room; }
    public void    setRoom(String v)           { this.room = v; }

    public String  getPot()                    { return pot; }
    public void    setPot(String v)            { this.pot = v; }

    public String  getLight()                  { return light; }
    public void    setLight(String v)          { this.light = v; }

    public String  getNotes()                  { return notes; }
    public void    setNotes(String v)          { this.notes = v; }
}

import java.util.HashMap;

public class Journal {
    private final Volume[] volumes;
    private final HashMap<String, Integer> authorIDs;
    
    //
    public Journal(Volume[] volumes, HashMap<String, Integer> authorIDs) {
        this.volumes = volumes;
        this.authorIDs = authorIDs;
    }
    
    //
    public Volume[] getVolumes() {
        return this.volumes;
    }
    
    public XmlDocument toXml(Volume volume) {
        return new XmlDocument(volume, this.authorIDs);
    }
}

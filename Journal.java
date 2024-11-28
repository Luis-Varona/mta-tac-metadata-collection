import java.util.HashMap;

public class Journal {
    private final Volume[] volumes;
    private final HashMap<String, Integer> authorIDs;
    
    /* 
     * Constructor for Journal class
     * assigns passed volumes array to volumes 
     * assigns passed authorIDs HashMap to volumes 
     * takes in: Volume[] volumes, HashMap<AuthorName, AuthorID> authorIDs
     */
    public Journal(Volume[] volumes, HashMap<String, Integer> authorIDs) {
        this.volumes = volumes;
        this.authorIDs = authorIDs;
    }
    
    // return value of volumes field
    public Volume[] getVolumes() {
        return this.volumes;
    }
    
    /* 
     * convert volume objects to XML Documents 
     * takes in: Volume object
     * returns: XMLDocument object created from passed volume obj & authorIDs field value
     */
    public XmlDocument toXml(Volume volume) {
        return new XmlDocument(volume, this.authorIDs);
    }
}

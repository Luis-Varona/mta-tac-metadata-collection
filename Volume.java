import java.util.*;

public class Volume {
    private final int volume, year;
    private final String title;
    private final Article[] articles;
    private final int[] fileIDs;
    
    //
    public Volume(Article[] articles, HashMap<Integer, String> volTitles, int fileIDFirst) {
        this.volume = articles[0].getVolume();
        
        assert Arrays.stream(articles).allMatch(a -> a.getVolume() == this.volume) :
            "All articles must belong to the same volume";
        
        for (int i = 0; i < articles.length - 1; i++) {
            assert articles[i].getEndPage() + 1 == articles[i + 1].getStartPage() :
                "Articles must be sorted by page range.";
        }
        
        this.year = articles[0].getYear();
        
        String token = volTitles.get(this.volume);
        this.title = token.matches("\\d+") ? null : token;
        
        this.articles = articles;
        this.fileIDs = new int[articles.length];
        
        for (int i = 0; i < articles.length; i++) {
            this.fileIDs[i] = fileIDFirst + i;
        }
    }
    
    //
    public int getVolume() { return this.volume; }
    public int getYear() { return this.year; }
    public String getTitle() { return this.title; }
    public Article[] getArticles() { return this.articles; }
    public int[] getFileIDs() { return this.fileIDs; }
}
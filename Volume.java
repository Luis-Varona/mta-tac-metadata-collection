import java.util.*;

public class Volume {
    private final int volume, year;
    private final String title;
    private final Article[] articles;
    private final int[] fileIDs;
    
    /* 
     * Constructor for Volume class
     * set volume number to volume of first article
     * verify that objects in articles all match that each article's volume equals this volume or let user know
     * loop through articles:
     *    assert that immediate article object after index end page value is same as immediate article object after index start page val or let user know
     * set year to year of first article
     * checks if volume title is a numeric string If so, set title to null; otherwise, title set to actual volume title
     * assign passed articles param to articles field
     * assign new int[] of length of articles to fileIDs field
     * loop through articles and increment value of filedIDs at index by fileIDFirst + index
     * takes in: Article[] articles, HashMap<Vol Num, Vol Titles> volTitles, int fileID
     */
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
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class TACMetadata {
    private static final String HOME = "http://www.tac.mta.ca/tac/";
    private static final LinkedList<String> HTML_LINES = initHtmlLines();
    private static final HashMap<Integer, String> VOL_TITLES = initVolTitles();
    private static final LinkedList<String> ABSTRACT_SOURCES = initAbstractSources();
    
    /* 
     * main method for class
     * initalize an array of Article objects articles
     * loop though Array, at index, initalize a new Article object from ABSTRACT_SOURCES
     * sort the articles array in ascending order; by volume then if in same volume by startPage.
     * create a Journal object using buildJournal method and passing it the articles array & authorIDs by callling getAuthorIDs
     * for each volume in the journal call toXml() to get XML data and save to file named according with the volume
     */
    public static void main(String[] args) throws IOException {
        int n = ABSTRACT_SOURCES.size();
        Article[] articles = new Article[n];
        
        for (int i = 0; i < n; i++) {
            articles[i] = new Article(ABSTRACT_SOURCES.get(i));
        }
        
        Arrays.sort(articles, Comparator.comparing(Article::getVolume)
                                        .thenComparing(Article::getStartPage));
        
        Journal theoryAndAppsOfCats = buildJournal(articles, getAuthorIDs(articles));
        
        for (Volume volume : theoryAndAppsOfCats.getVolumes()) {
            theoryAndAppsOfCats.toXml(volume).saveToFile(
                String.format("metadata/TAC_vol%02d.xml", volume.getVolume()));
        }
    }
    
    /* 
     * test for input/output errors & URI Syntax erros while 
     * creating new connection to tac home page
     * streaming HTML snippets into htmlLines linked list
     * split into indv lines using regex, then as they are added remove any whitespaces
     * and these new strings to the collection then disconnect
     * return htmlLines
     */
    private static LinkedList<String> initHtmlLines() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URI(HOME).toURL().openConnection();
            
            LinkedList<String> htmlLines = Arrays.stream(new String(
                con.getInputStream()
                .readAllBytes())
                .split("\n"))
                .map(String::strip)
                .collect(Collectors.toCollection(LinkedList::new));
            
            con.disconnect();
            return htmlLines;
        }
        catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    /* 
     * establishes the standard notation
     * instantitate regex patterns for volume, article num & title
     * extracts vol nums & titles from HTML lines and stores them in HashMap:
     * find all occurrences of  volume pattern in current HTML line using volMatcher
     * for each occurrence, extract volume number using numMatcher and convert to an integer
     * then extract corresponding title using titleMatcher
     * the volume number & title are added to the volTitles HashMap.
     * returns: HashMap of volTitles
     */

    private static HashMap<Integer, String> initVolTitles() {
        HashMap<Integer, String> volTitles = new HashMap<>();
        
        Pattern volPattern = Pattern.compile("Vol[.] \\d+");
        Pattern numPattern = Pattern.compile("\\d+");
        Pattern titlePattern = Pattern.compile("[-]\\s[^<]+</a>");
        
        Iterator<String> htmlIter = HTML_LINES.iterator();
        String line = htmlIter.next();
        
        while (!volPattern.matcher(line).find()) {
            line = htmlIter.next();
        }
        
        Matcher volMatcher = volPattern.matcher(line);
        
        while (volMatcher.find()) {
            Matcher numMatcher = numPattern.matcher(volMatcher.group());
            numMatcher.find();
            int volNum = Integer.parseInt(numMatcher.group());
            
            Matcher titleMatcher = titlePattern.matcher(line);
            titleMatcher.find();
            String volTitle = titleMatcher.group()
                .substring(2, titleMatcher.group().length() - 4);
            
            volTitles.put(volNum, volTitle);
            line = htmlIter.next();
            volMatcher = volPattern.matcher(line);
        }
        
        return volTitles;
    }
    
    /* 
     * iterate through lines of HTML_LINES; if lines contain substring then split + add to pages HashSet
     * iterate over page obj of pages HashSet; HttpURLConnection to connect to a URL constructed from HOME string & current page
     * reads the bytes from the connection's input stream and add the resulting string of the InputStream & bytes of said page to abstractSources
     * throw an err if input/output exception or URISyntax exception
     * returns: abstractSources LinkedList of Strings of sources (ahref links)
     */

    private static LinkedList<String> initAbstractSources() {
        HashSet<String> pages = new HashSet<>();
        LinkedList<String> abstractSources = new LinkedList<>();
        
        for (String line : HTML_LINES) {
            if (line.contains("abs.html")) {
                pages.add(line.split("\"")[1]);
                
            }
        }
        
        for (String page : pages) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URI(HOME + page).toURL()
                    .openConnection();
                abstractSources.add(new String(con.getInputStream().readAllBytes()));
                con.disconnect();
            }
            catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        
        return abstractSources;
    }
    
    /* 
     * generates a HashMap of Authors from the articles provided and generates an id value for them
     * params: takes in article object from Article array 
     * (might be helpful to switch the naming structure around, i.e. Articles[] article
     * returns: authorID HashMap (Author, Author ID Number)
     */

    private static HashMap<String, Integer> getAuthorIDs(Article[] articles) {
        HashMap<String, Integer> authorIDs = new HashMap<>();
        int id = 1;
        
        for (Article article : articles) {
            for (String author : article.getAuthors()) {
                if (!authorIDs.containsKey(author)) {
                    authorIDs.put(author, id++);
                }
            }
        }
        
        return authorIDs;
    }
    
    /* 
     * loop through VOL_TITLES (num & volume title obj)
     * get the corresponding article data from articles stream & add html snippets to volumeArticles ArrayList
     * update volumes (array of article obj) with volume objects that comprise of Article[] objects from volumeArticles,
     * that volume's title from VOL_TITLES, and a fileID num generate from the count
     * takes in: article array & HashMap of authorIDs
     * returns a new journal object of volumes & authorIDs
     */
    private static Journal buildJournal(Article[] articles,
                                        HashMap<String, Integer> authorIDs) {
        int k = VOL_TITLES.size();
        Volume[] volumes = new Volume[k];
        Iterator<Article> articleIter = Arrays.stream(articles).iterator();
        Article article = articleIter.next();
        int fileIDFirst = 1;
        
        for (int i = 0; i < k; i++) {
            ArrayList<Article> volumeArticles = new ArrayList<>();
            int ct = 0;
            
            while (article.getVolume() == i + 1) {
                volumeArticles.add(article);
                ct++;
                
                if (articleIter.hasNext()) {
                    article = articleIter.next();
                }
                else {
                    break;
                }
            }
            
            volumes[i] = new Volume(volumeArticles.toArray(Article[]::new),
                                    VOL_TITLES, fileIDFirst);
            fileIDFirst += ct;
        }
        
        return new Journal(volumes, authorIDs);
    }
}
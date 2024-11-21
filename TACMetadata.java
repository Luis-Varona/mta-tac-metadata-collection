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
    
    //
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
    
    //
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
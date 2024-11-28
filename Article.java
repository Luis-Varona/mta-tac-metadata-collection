import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class Article {
    private final LinkedList<String> htmlLines;
    private final String pdfSource;
    private final int fileSize;
    private final String name;
    private final String title;
    private final String[] authors;
    private final String abstractText;
    private final String[] keywords;
    private final int volume, year;
    private final int startPage, endPage;
    
    /* 
     * Article constructor
     * stream htmlSource into an Array htmlLines, split by new line, strip whitespace & store in LinkedList
     * call the appropriate methods to get specific metadata then assign to corresponding fields of Article object
     */
    public Article(String htmlSource) {
        this.htmlLines = Arrays.stream(htmlSource.split("\n"))
                .map(String::strip)
                .collect(Collectors.toCollection(LinkedList::new));
        
        this.pdfSource = initPdfSource();
        this.fileSize = initFileSize();
        this.name = initName();
        
        this.title = initTitle();
        this.authors = initAuthors();
        this.abstractText = initAbstract();
        this.keywords = initKeywords();
        
        IssueIdent issue = initIssueIdent();
        this.volume = issue.volume();
        this.year = issue.year();
        
        PageRange pages = initPages();
        this.startPage = pages.startPage();
        this.endPage = pages.endPage();
    }
    
    //
    public String getPdfSource() { return this.pdfSource; }
    public int getFileSize() { return this.fileSize; }
    public String getName() { return this.name; }
    
    public String getTitle() { return this.title; }
    public String[] getAuthors() { return this.authors; }
    public String getAbstract() { return this.abstractText; }
    public String[] getKeywords() { return this.keywords; }
    
    public int getVolume() { return this.volume; }
    public int getYear() { return this.year; }
    public int getStartPage() { return this.startPage; }
    public int getEndPage() { return this.endPage; }
    
    /* 
     * Override toString method for our purposes; stringifying author names based on num of authors
     * loop num of authors; initialize a String[] names of String objects from authors[i] split the strings at " "
     * assign last elem of names[] to lastNames[i]
     * depending on k:
     *    case 1) if = 1, assign lastNames[0] to authorField
     *    case 2) if = 2, assign 1st & 2nd last name to authorField using format `last_name_1 and last_name_2`
     *    case 3) if = 3 using String builder to:
     *       append first two last names to sb, seperated by ", " then append third last name with "and "
     * if k isn't case 1-3 then assign authorField to String with format 1st last name and " et al."
     * return String in format title (authorField, year)
     */
    @Override
    public String toString() {
        String authorField;
        int k = this.authors.length;    
        String[] lastNames = new String[k];
        
        for (int i = 0; i < this.authors.length; i++) {
            String[] names = this.authors[i].split(" ");
            lastNames[i] = names[names.length - 1];
        }
        
        switch (k) {
            case 1 -> authorField = lastNames[0];
            case 2 -> authorField = String.format("%s and %s", lastNames[0], lastNames[1]);
            case 3 -> {
                StringBuilder sb = new StringBuilder();
                
                for (int i = 0; i < 2; i++) {
                    sb.append(lastNames[i]).append(", ");
                }
                
                sb.append("and ").append(lastNames[2]);
                authorField = sb.toString();
            }
            default -> authorField = String.format("%s et al.", lastNames[0]);
        }
        
        return String.format("%s (%s %d)", this.title, authorField, this.year);
    }
    
    /* 
     * Iterate over htmlLines to find pdf file names; if a line doesn't contain "citation_pdf_url" & matches the pattern: [any char][any digit][.]["pdf"][any char]
     * split lines by double quotes ("), extract string between quotes and assign to init, then break
     * if init is null, iterate over htmlLines, if line matches pattern [any char][any digit][.]["dvi" OR "ps"][any char]:
     *    split lines by double quotes ("), extract string between quotes and assign to init
     *    then replace any notion of [.]"dvi" OR [.]"ps" with ".pdf", then break
     * returns String init, which is pdf file name 
     */
    private String initPdfSource() {
        String init = null;
        
        for (String line : this.htmlLines) {
            if (!line.contains("citation_pdf_url") && line.matches(".*\\d[.]pdf.*")) {
                init = line.split("\"")[1];
                break;
            }
        }
        
        if (init == null) {
            for (String line : this.htmlLines) {
                if (line.matches(".*\\d[.](dvi|ps).*")) {
                    init = line.split("\"")[1];
                    init = init.replaceAll("[.](dvi|ps)", ".pdf");
                    break;
                }
            }
        }
        
        return init;
    }
    
    /* 
     * create a HttpURLConnection from pdfSource, once Connection is open get server metadata & length of said metadata (init)
     * catch any input/output exceptions or URISyntaxException errors
     * returns: init Int, file size
     */
    private int initFileSize() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URI(this.pdfSource).toURL()
            .openConnection();
            con.setRequestMethod("HEAD");
            int init = con.getContentLength();
            con.disconnect();
            return init;
        }
        catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    /* 
     * initiate a String[] of strings (URL paths) from pdfSource split on the "/"
     * returns: last elem of string[]  
     */
    private String initName() {
        String[] href = this.pdfSource.split("/");
        return href[href.length - 1];
    }
    
    /* 
     * extracting the text between the first <h1> tags in the HTML by:
     *    1. iterating over htmlLines, skip lines until an <h1> tag is found, move to the next line.
     *    2. Append lines to a StringBuilder `sb` with a " " until a </h1> tag is found.
     * init String comprises: replacing any whitespace surrounding <p> or </> with " ", then stripChars with " ," substring
     * returns: hard coded title corrections from TITLE_CORRECTIONS, getOrDefault gets val associated with key init or replaces with init val
     */
    private String initTitle() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> htmlIter = this.htmlLines.iterator();
        String line = htmlIter.next();
        
        while (!line.contains("<h1>")) {
            line = htmlIter.next();
        }
        
        line = htmlIter.next();
        
        while (!line.contains("</h1>")) {
            sb.append(line).append(" ");
            line = htmlIter.next();
        }
        
        String init = stripChars(sb.toString().replaceAll("\\s+|<p>|</p>", " "), " ,");
        return Corrections.TITLE_CORRECTIONS.getOrDefault(init, init);
    }
    
    /* 
     * extracting the text between the first <h1> and <h2> tags in the HTML by:
     *    1. iterating over htmlLines, skip lines until an </h1> tag is found, move to the next line.
     *    2. Skip empty lines or lines containing <h2> tags.
     *    3. Append lines to a StringBuilder `sb` with a " " until a <h2> or </h2> tag is found.
     * authorList String[] is created by converting `sb` to string, replacing " and "  with "," & split on "," 
     * for strings in in authorList:
     *    1. create author string from stripChars() on " ," substrings, replacing all extra whitespaces with "", adding a space after "." between two letters.
     * if author is not an "":
     *    1. if author string is "Jr.", replace last val of init with ", Jr." OR if not just add to init
     * return: converted init to an Array of Strings
     */
    private String[] initAuthors() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> htmlIter = this.htmlLines.iterator();
        String line = htmlIter.next();
        
        while (!line.contains("</h1>")) {
            line = htmlIter.next();
        }
        
        line = htmlIter.next();
        
        while (line.equals("") || line.contains("<h2>")) {
            line = htmlIter.next();
        }
        
        while(!line.contains("<h2>") && !line.contains("</h2>")) {
            sb.append(line).append(" ");
            line = htmlIter.next();
        }
        
        String[] authorList = sb.toString().replaceAll(" and ", ",").split(",");
        LinkedList<String> init = new LinkedList<>();
        
        for (String s : authorList) {
            String author = stripChars(s, " ,")
                .replaceAll("\\s+", " ")
                .replaceAll("([A-Za-z])[.]([A-Za-z]+)", "$1. $2");
            
            if (!author.equals("")) {
                
                if (author.equals("Jr.")) {
                    init.add(init.removeLast() + ", Jr.");
                }
                else {
                    init.add(author);
                }
            }
        }
        
        return init.toArray(String[]::new);
    }
    
    /* 
     * iterate over htmlLines until a line contains "</h2>", "<p>", or "Keywords:"
     * if a line doesn't contain "Keywords:" then add the line to StringBuilder `sb`  and a " " before moving to next
     * abstractInit is created by converting `sb` to string, replacing "<p>" or "</p>" with " ", strip whitespaces, 
     * replacing any whitespaces with " ", & if a <br> tag is surrounded by whitespace or adjacent with whitespace, replace with single <br>
     * with the changes iterate again until  a line contains "Keywords:", a <p> while ignoring </p>
     * if a line does not contain the string "</p>" or is not exactly equal to "</p>" then add the line to StringBuilder `sb`  and a " " before moving to next
     * now we can create classifInit from `sb` by replacing "<p>" or "</p>" with " ", strip whitespaces, 
     * replacing ", with ", " & andy whitespaces with a singular " "
     * if classifInit does not end with ".", add "."
     * now creating `sb` correctly by surrounding abstractInit and classifInit with opening and closing <p> tags
     * returns: `sb` as a String
     */
    private String initAbstract() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> htmlIter = this.htmlLines.iterator();
        String line = htmlIter.next();
        
        while (!line.contains("</h2>")) {
            line = htmlIter.next();
        }
        
        while (!line.contains("<p>")) {
            line = htmlIter.next();
        }
        
        line = htmlIter.next();
        
        while (!line.contains("Keywords:")) {
            sb.append(line).append(" ");
            line = htmlIter.next();
        }
        
        String abstractInit = sb
            .toString()
            .replaceAll("<p>|</p>", " ")
            .strip()
            .replaceAll("\\s+", " ")
            .replaceAll("\\s<br>\\s|\\s<br>|<br>\\s", "<br>");
        
        sb.setLength(0);
        
        while(!line.contains("Keywords:")) {
            line = htmlIter.next();
        }
        
        line = htmlIter.next();
        
        while (!line.contains("<p>") || line.equals("</p>")) {
            line = htmlIter.next();
        }
        
        while (!line.contains("</p>") || !line.equals("</p>")) {
            sb.append(line).append(" ");
            line = htmlIter.next();
        }
        
        String classifInit = sb.toString()
            .replaceAll("<p>|</p>", " ")
            .strip()
            .replaceAll(",", ", ")
            .replaceAll("\\s+", " ");
        
        sb.setLength(0);
        
        if (!classifInit.endsWith(".")) {
            classifInit += ".";
        }
        
        sb.append("<p>")
            .append(abstractInit)
            .append("</p><p>")
            .append(classifInit)
            .append("</p>");
        
        return sb.toString();
    }
    
    /* 
     * iterate over htmlLines til line contains "Keywords:" but does not end with it
     * add the line to StringBuilder `sb` and a space
     * once a </p> html tag is found, add the line to `sb` and a space
     * create an Array of Strings `keywordList` from splitting `sb` at either the ',' or ';'
     * for each String in keywordList:
     *    1. replace all subtrings of "Keywords:", or "<p>", or "</p>" with ", "
     *    2. use stripChars() to remove any occurrences of " ." from the beginning and end of string
     *    3. replace any whitespace chars with " " 
     *    4. if a hyphen is surrounded by whitespace or adjacent with whitespace, replace with single hyphen
     * if a keyword is not "" & if our LinkedList is both !empty && its last node ends with "-"; add the last node + keyword to the LinkedList
     * else: just add the keyword
     * then create a String[] with the values from init
     * returns string[]
     */
    private String[] initKeywords() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> htmlIter = this.htmlLines.iterator();
        String line = htmlIter.next();
        
        while (!line.contains("Keywords:")) {
            line = htmlIter.next();
        }
        
        if (line.endsWith("Keywords:")) {
            line = htmlIter.next();
        }
        
        sb.append(line).append(" ");
        
        while (!line.contains("</p>")) {
            sb.append(line).append(" ");
            line = htmlIter.next();
        }
        
        String[] keywordList = sb.toString().split(",|;");
        LinkedList<String> init = new LinkedList<>();
        
        for (String s : keywordList) {
            String keyword = stripChars(s.replaceAll("Keywords:|<p>|</p>", ""), " .")
                .replaceAll("\\s+", " ")
                .replaceAll("\s[-]\s|[-]\s|\s[-]", "-");
            
            if (!keyword.equals("")) {
                if (!init.isEmpty() && init.getLast().endsWith("-")) {
                    init.add(init.removeLast() + keyword);
                }
                else {
                    init.add(keyword);
                }
            }
        }
        
        return init.toArray(String[]::new);
    }
    
    /* 
     * iterate over htmlLines LinkedList; if line doesn't contain Strings "Keywords:" or "Vol.", or ends with "Keywords:" move to next
     * iterator issueInfo splits array lines at blank space, String elem is issueInfo line after removing any occurrences of " ," from the beginning and end of string
     * continue stripChars() til "Vol." is all that remains
     * volumeInit follows same logic; parse String issueInfo as an Int, remove any occurrences of " ," from the beginning and end of string
     * same for yearElem though if the string starts with "CT" start parsing as Int at index 3
     * create a new issueIdent obj of VolumeInit, yearInit
     * returns: initIssueIdent
     */
    private IssueIdent initIssueIdent() {
        Iterator<String> htmlIter = this.htmlLines.iterator();
        String line = htmlIter.next();
        
        while (!line.contains("Keywords:")) {
            line = htmlIter.next();
        }
        
        if (line.endsWith("Keywords:")) {
            line = htmlIter.next();
        }
        
        while (!line.contains("Vol.")) {
            line = htmlIter.next();
        }
        
        Iterator<String> issueInfo = Arrays.asList(line.split(" ")).iterator();
        String elem = stripChars(issueInfo.next(), " ,");
        
        while (!elem.equals("Vol.")) {
            elem = stripChars(issueInfo.next(), " ,");
        }
        
        int volumeInit = Integer.parseInt(stripChars(issueInfo.next(), " ,"));
        int yearInit;
        String yearElem = stripChars(issueInfo.next(), " ,");
        
        if (yearElem.startsWith("CT")) {
            yearInit = Integer.parseInt(yearElem.substring(3));
        }
        else {
            yearInit = Integer.parseInt(yearElem);
        }
        
        return new IssueIdent(volumeInit, yearInit);
    }
    
    /* 
     * check if any pages need corrections from Corrections
     * initalize page pattern reg ex for method to recognize i.e. "pp X-Y", "ppX-Y", "pp. X-Y", "pp.X-Y", "pp .X-Y"
     * match pattern against htmlLines
     * new matcher pageMatch finds pattern of 1+ digits, followed by a hyphen, then 1+ digits; find page numbers amongst page patterns
     * find next subsequence that matches the sequence input of pageMatch 
     * group all those page sequences together in String array pagelist, split at hyphen
     * update init record with PageRange which consists of first and last integer found in pageList String at index
     * returns: PageRange record init
     */
    private PageRange initPages() {
        PageRange init;
        
        if (Corrections.PAGE_CORRECTIONS.containsKey(this.title)) {
            init = Corrections.PAGE_CORRECTIONS.get(this.title);
        }
        else {
            Iterator<String> htmlIter = this.htmlLines.iterator();
            Pattern pattern = Pattern.compile(String.format("(%s|%s|%s|%s|%s)",
                                                    "pp \\d+-+\\d+",
                                                            "pp\\d+-+\\d+",
                                                            "pp[.] \\d+-+\\d+",
                                                            "pp[.]\\d+-+\\d+",
                                                            "pp [.]\\d+-+\\d+"));
            Matcher matcher = pattern.matcher(htmlIter.next());
            
            while (!matcher.find()) {
                matcher = pattern.matcher(htmlIter.next());
            }
            
            Matcher pageMatch = Pattern.compile("\\d+-+\\d+").matcher(matcher.group());
            pageMatch.find();
            String[] pageList = pageMatch.group().split("-");
            init = new PageRange(Integer.parseInt(pageList[0]),
                                 Integer.parseInt(pageList[pageList.length - 1]));
        }
        
        return init;
    }
    
    /* 
     * remove any occurrences of sequences in chars from the beginning and end of s string
     * repeatedly remove first character of s if it matches any character in chars
     * remove last character of s if any sequence in chars is present in s.
     * takes in: String s & String chars
     * returns: String s
     */
    private String stripChars(String s, String chars) {
        while (s.matches(String.format("[%s].*", chars))) {
            s = s.substring(1);
        }
        
        while (s.matches(String.format(".*[%s]", chars))) {
            s = s.substring(0, s.length() - 1);
        }
        
        return s;
    }
}

record Corrections() {
    public static final HashMap<String, String> TITLE_CORRECTIONS = new HashMap<>() {{
        put("Functorial and algebraic properties of Browns P functor",
            "Functorial and algebraic properties of Brown's P functor");
        put("Approximable Concepts, Chu spaces, and information systems",
            "Approximable concepts, Chu spaces, and information systems");
    }};
    
    public static final HashMap<String, PageRange> PAGE_CORRECTIONS = new HashMap<>() {{
        put("Functorial and algebraic properties of Brown's P functor",
            new PageRange(10, 53));
        put("Kan extensions along promonoidal functors", new PageRange(72, 77));
        put("A forbidden-suborder characterization of binarily-composable diagrams " +
            "in double categories", new PageRange(146, 155));
        put("Doctrines whose structure forms a fully faithful adjoint string",
            new PageRange(24, 44));
        put("Multilinearity of Sketches", new PageRange(269, 277));
        put("Distributive laws for pseudomonads", new PageRange(91, 147));
        put("Normal functors and strong protomodularity", new PageRange(206, 218));
        put("On the object-wise tensor product of functors to modules",
            new PageRange(227, 235));
        put("Algebraically closed and existentially closed substructures " +
            "in categorical context", new PageRange(270, 298));
        put("Approximable concepts, Chu spaces, and information systems",
            new PageRange(80, 102));
        put("Quotients of unital $A_\\infty$-categories",
            new PageRange(405, 496));
        put("The Fa&agrave; di Bruno construction", new PageRange(394, 425));
        put("On the monad of internal groupoids", new PageRange(150, 165));
        put("Complicial structures in the nerves of omega-categories",
            new PageRange(780, 803));
        put("A Bayesian characterization of relative entropy", new PageRange(422, 456));
        put("The weakly globular double category of fractions of a category",
            new PageRange(696, 774));
        put("An algebraic definition of ($\\infty$,n)-categories", new PageRange(775, 807));
        put("On reflective subcategories of locally presentable categories",
            new PageRange(1306, 1318));
        put("Stacks and sheaves of categories as fibrant objects, II",
            new PageRange(330, 364));
        put("A note on injective hulls of posemigroups", new PageRange(254, 257));
        put("A bicategory of decorated cospans", new PageRange(995, 1027));
        put("A construction of certain weak colimits and an exactness property " +
            "of the 2-category of categories", new PageRange(193, 215));
        put("Crossed products of crossed modules of Hopf monoids", new PageRange(867, 897));
    }};
}

record IssueIdent(int volume, int year) {}
record PageRange(int startPage, int endPage) {}
import java.io.*;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.HashMap;

public class XmlDocument {
    private static final String W3_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String PKP_HOME = "https://pkp.sfu.ca";
    
    private final Volume volume;
    private final HashMap<String, Integer> authorIDs;
    private final String dateCreated;
    private final String document;
    
    //
    public XmlDocument(Volume volume, HashMap<String, Integer> authorIDs) {
        this.volume = volume;
        this.authorIDs = authorIDs;
        this.dateCreated = initDateCreated();
        this.document = buildDocument();
    }
    
    //
    public int getVolume() {
        return this.volume.getVolume();
    }
    
    public void saveToFile(String path) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.createNewFile();
        Files.writeString(file.toPath(), this.document);
    }
    
    @Override
    public String toString() {
        return this.document;
    }
    
    //
    private String initDateCreated() {
        ZonedDateTime now = ZonedDateTime.now();
        return String.format("%d-%02d-%02d",
                             now.getYear(),
                             now.getMonthValue(),
                             now.getDayOfMonth());
    }
    
    private String buildDocument() {
        StringBuilder doc = new StringBuilder(192000);
        doc.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        doc.append(String.format("<articles xmlns:xsi=\"%s\" ", W3_SCHEMA));
        doc.append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        
        Article[] articles = this.volume.getArticles();
        int fileID = this.volume.getFileIDs()[0];
        int i = 0;
        
        for (Article article : articles) {
            doc.append(buildXmlBlock(article, fileID + i, i)).append("\n");
            i++;
        }
        
        doc.delete(doc.length() - 1, doc.length()).append("</articles>");
        return doc.toString();
    }
    
    private String buildXmlBlock(Article article, int fileID, int seqInVol) {
        String[] authors = article.getAuthors();
        StringBuilder block = new StringBuilder(3200);
        
        block.append(String.format("\s\s<article xmlns=\"%s\" ", PKP_HOME))
            .append(String.format("xmlns:xsi=\"%s\" locale=\"en\" ", W3_SCHEMA))
            .append(String.format("date_submitted=\"%s\" status=\"3\" ", dateCreated))
            .append("submission_progress=\"\" current_publication_id=\"1\" stage=\"production\" ")
            .append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        
        block.append("\s\s\s\s<id type=\"internal\" advice=\"ignore\">")
            .append(String.format("%d</id>\n", fileID));
        
        block.append("\s\s\s\s<submission_file ")
            .append(String.format("xmlns:xsi=\"%s\" id=\"%d\"", W3_SCHEMA, fileID))
            .append(String.format("created_at=\"%s\" date_created=\"\" ", dateCreated))
            .append(String.format("file_id=\"%d\" stage=\"submission\" ", fileID))
            .append(String.format("updated_at=\"%s\" viewable=\"true\" ", dateCreated))
            .append("genre=\"Article Text\" ")
            .append(String.format("source_submission_file_id=\"%d\" ", fileID))
            .append("uploader=\"admin\" ")
            .append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        
        block.append("\s\s\s\s\s\s<name locale=\"en\">")
            .append(String.format("%s</name>\n", article.getName()));
        block.append(String.format("\s\s\s\s\s\s<file id=\"%d\" ", fileID))
            .append(String.format("filesize=\"%d\" ", article.getFileSize()))
            .append("extension=\"pdf\">\n");
        block.append("\s\s\s\s\s\s\s\s<href src=")
            .append(String.format("\"%s\"/>\n", article.getPdfSource()));
        block.append("\s\s\s\s\s\s</file>\n");
        block.append("\s\s\s\s</submission_file>\n");
        
        block.append(String.format("\s\s\s\s<publication xmlns:xsi=\"%s\" ", W3_SCHEMA))
            .append("version=\"1\" status=\"3\" ")
            .append("primary_contact_id=")
            .append(String.format("\"%d\" url_path=\"\" ", this.authorIDs.get(authors[0])))
            .append(String.format("seq=\"%d\" access_status=\"0\" ", seqInVol))
            .append(String.format("date_published=\"%s\" section_ref=\"ART\" ", dateCreated))
            .append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        
        block.append("\s\s\s\s\s\s<id type=\"internal\" advice=\"ignore\">")
            .append(String.format("%d</id>\n", fileID));
        block.append("\s\s\s\s\s\s<id type=\"doi\" advice=\"update\">10.1119/5.0158200</id>\n");
        block.append("\s\s\s\s\s\s<title locale=\"en\">")
            .append(String.format("%s</title>\n", article.getTitle()));
        block.append("\s\s\s\s\s\s<abstract locale=\"en\">")
            .append(String.format("%s</abstract>\n", article.getAbstract()));
        block.append("\s\s\s\s\s\s<licenseURL>http://www.tac.mta.ca/tac/consent.html")
            .append("</licenseURL>\n");
        block.append("\s\s\s\s\s\s<copyrightHolder locale=\"en\">author</copyrightHolder>\n");
        block.append("\s\s\s\s\s\s<copyrightYear>")
            .append(String.format("%d</copyrightYear>\n", this.volume.getYear()));
        
        block.append("\s\s\s\s\s\s<keywords locale=\"en\">\n");
        
        for (String keyword : article.getKeywords()) {
            block.append(String.format("\s\s\s\s\s\s\s\s<keyword>%s</keyword>\n", keyword));
        }
        
        block.append("\s\s\s\s\s\s</keywords>\n");
        
        block.append(String.format("\s\s\s\s\s\s<authors xmlns:xsi=\"%s\" ", W3_SCHEMA))
            .append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        int i = 0;
        
        for (String author : authors) {
            Author a = separateAuthorNames(author);
            
            block.append("\s\s\s\s\s\s\s\s<author include_in_browse=\"true\" ")
                .append(String.format("user_group_ref=\"Author\" seq=\"%d\" ", i++))
                .append(String.format("id=\"%d\">\n", this.authorIDs.get(author)));
            block.append("\s\s\s\s\s\s\s\s\s\s<givenname>")
                .append(String.format("%s</givenname locale=\"en\">\n", a.givenName()));
            block.append("\s\s\s\s\s\s\s\s\s<familyname locale=\"en\">")
                .append(String.format("%s</surname>\n", a.familyName()));
            block.append("\s\s\s\s\s\s\s\s\s\s<email>madeup@email.org</email>\n");
            block.append("\s\s\s\s\s\s\s\s</author>\n");
        }
        
        block.append("\s\s\s\s\s\s</authors>\n");
        
        block.append("\s\s\s\s\s\s<article_galley ")
            .append(String.format("xmlns:xsi=\"%s\" locale=\"en\" url_path=\"\" ", W3_SCHEMA))
            .append("approved=\"false\" ")
            .append(String.format("xsi:schemaLocation=\"%s native.xsd\">\n", PKP_HOME));
        block.append("\s\s\s\s\s\s\s\s<id type=\"internal\" advice=\"ignore\">")
            .append(String.format("%d</id>\n", fileID));
        block.append("\s\s\s\s\s\s\s\s<name locale=\"en\">PDF</name>\n");
        block.append(String.format("\s\s\s\s\s\s\s\s<seq>%d</seq>\n", seqInVol));
        block.append("\s\s\s\s\s\s\s\s<submission_file_ref ")
            .append(String.format("id=\"%d\"/>\n", fileID));
        block.append("\s\s\s\s\s\s</article_galley>\n");
        
        block.append("\s\s\s\s\s\s<issue_identification>\n");
        block.append("\s\s\s\s\s\s\s\s<volume>")
            .append(String.format("%d</volume>\n", this.volume.getVolume()));
        block.append("\s\s\s\s\s\s\s\s<year>")
            .append(String.format("%d</year>\n", this.volume.getYear()));
        String volumeTitle = this.volume.getTitle();
        
        if (volumeTitle != null) {
            block.append("\s\s\s\s\s\s\s\s<title locale=\"en\">")
                .append(String.format("%s</title>\n", volumeTitle));
        }
        
        block.append("\s\s\s\s\s\s</issue_identification>\n");
        
        block.append("\s\s\s\s\s\s<pages>")
            .append(String.format("%d-%d", article.getStartPage(), article.getEndPage()))
            .append("</pages>\n");
        
        block.append("\s\s\s\s</publication>\n");
        block.append("\s\s</article>\n");
        
        return block.toString();
    }
    
    private Author separateAuthorNames(String author) {
        int i = author.lastIndexOf(' ');
        return new Author(author.substring(0, i), author.substring(i + 1));
    }
}

record Author(String givenName, String familyName) {}
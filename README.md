# mta-tac-metadata-collection

Collecting metadata for Mount Allison University's [*Theory and Applications of Categories*](http://www.tac.mta.ca/tac/) journal. Preparing an XML file on each volume for the Public Knowledge Project's [*Open Journal Systems* database](https://pkp.sfu.ca/software/ojs/).

## How it works
- listcle stuff here

## Task List

- [ ] Figure out proper version/revision numbering (currently set to 1 by default)
- [ ] Fix mapping from `<br>` and `<p>` (in the abstracts) to escape sequences
- [ ] Add a new tag for MSC classification (currently placed in `<abstract>`)
- [ ] Verify separation of `<givenname>` and `<familyname>` for relevant articles (this will likely require human intuition, although we can automate the flagging of articles that require review)
- [ ] After all this is done, import everything in the OJS system and we are finished!


## Project Structure

```
.
└── mta-tac-metadata-collection
    ├── Article.java
    ├── Journal.java
    ├── README.md
    ├── TACMetadata.java
    ├── Volume.java
    ├── XmlDocument.java
    └── metadata
        ├── TAC_vol01.xml
        ├── TAC_vol02.xml
        ├── TAC_vol03.xml
        ├── TAC_vol04.xml
        └── ...
```
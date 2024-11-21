# mta-tac-metadata-collection

Collecting metadata for Mount Allison University's *Theory and Applications of Categories* journal. Preparing an XML file on each volume for the Public Knowledge Project's *Open Journal Systems* database. **STATUS:**

1. Figure out proper version/revision numbering (currently set to 1 by default)
2. Fix mapping from `<br>` and `<p>` (in the abstracts) to escape sequences
3. Add a new tag for MSC classification (currently placed in `<abstract>`)
4. Verify separation of `<givenname>` and `<familyname>` for relevant articles (this will likely require human intuition, although we can automate the flagging of articles that require review)
5. After all this is done, import everything in the OJS system and we are finished!

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

public class ImportTest {
    private ImportFormatPreferences pf;
    private BibEntryWriter bew;

    @Before
    public void setUp() {
        pf = JabRefPreferences.getInstance().getImportFormatPreferences();
        bew = new BibEntryWriter(new LatexFieldFormatter(JabRefPreferences.getInstance().getLatexFieldFormatterPreferences()), new BibEntryTypesManager());
    }

    @Test
    public void teste() {
        try (FileInputStream fis = new FileInputStream("src/test/java/Teste1.bib");
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            ParserResult pr = new BibtexParser(pf, new DummyFileUpdateMonitor()).parse(isr);
            BibDatabase db = pr.getDatabase();
            db.getEntries();

            StringWriter sw = new StringWriter();

            bew.write(db.getEntries().get(0), sw, BibDatabaseMode.BIBTEX);
            String s = sw.toString().trim();
            System.out.println(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


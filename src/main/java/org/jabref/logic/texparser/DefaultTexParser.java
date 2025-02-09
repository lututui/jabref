package org.jabref.logic.texparser;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.texparser.TexParser;
import org.jabref.model.texparser.TexParserResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTexParser implements TexParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTexParser.class);
    private static final String TEX_EXT = ".tex";

    /**
     * It is allowed to add new cite commands for pattern matching.
     * Some valid examples: "citep", "[cC]ite", and "[cC]ite(author|title|year|t|p)?".
     */
    private static final String[] CITE_COMMANDS = {
            "[cC]ite(alt|alp|author|authorfull|date|num|p|t|text|title|url|year|yearpar)?",
            "([aA]|[aA]uto|fnote|foot|footfull|full|no|[nN]ote|[pP]aren|[pP]note|[tT]ext|[sS]mart|super)cite",
            "footcitetext", "(block|text)cquote"
    };
    private static final String CITE_GROUP = "key";
    private static final Pattern CITE_PATTERN = Pattern.compile(
            String.format("\\\\(%s)\\*?(?:\\[(?:[^\\]]*)\\]){0,2}\\{(?<%s>[^\\}]*)\\}(?:\\{[^\\}]*\\})?",
                    String.join("|", CITE_COMMANDS), CITE_GROUP));

    private static final String INCLUDE_GROUP = "file";
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
            String.format("\\\\(?:include|input)\\{(?<%s>[^\\}]*)\\}", INCLUDE_GROUP));

    private final TexParserResult texParserResult;

    public DefaultTexParser() {
        this.texParserResult = new TexParserResult();
    }

    public TexParserResult getTexParserResult() {
        return texParserResult;
    }

    @Override
    public TexParserResult parse(String citeString) {
        matchCitation(Paths.get(""), 1, citeString);
        return texParserResult;
    }

    @Override
    public TexParserResult parse(Path texFile) {
        return parse(Collections.singletonList(texFile));
    }

    @Override
    public TexParserResult parse(List<Path> texFiles) {
        texParserResult.addFiles(texFiles);
        List<Path> referencedFiles = new ArrayList<>();

        for (Path file : texFiles) {
            if (!file.toFile().exists()) {
                LOGGER.error(String.format("File does not exist: %s", file));
                continue;
            }

            try (LineNumberReader lineNumberReader = new LineNumberReader(Files.newBufferedReader(file))) {
                for (String line = lineNumberReader.readLine(); line != null; line = lineNumberReader.readLine()) {
                    // Skip comments and blank lines.
                    if (line.trim().isEmpty() || line.trim().charAt(0) == '%') {
                        continue;
                    }
                    matchCitation(file, lineNumberReader.getLineNumber(), line);
                    matchNestedFile(file, texFiles, referencedFiles, line);
                }
            } catch (ClosedChannelException e) {
                LOGGER.error("Parsing has been interrupted");
                return null;
            } catch (IOException | UncheckedIOException e) {
                LOGGER.error(String.format("%s while parsing files: %s", e.getClass().getName(), e.getMessage()));
            }
        }

        // Parse all files referenced by TEX files, recursively.
        if (!referencedFiles.isEmpty()) {
            parse(referencedFiles);
        }

        return texParserResult;
    }

    /**
     * Find cites along a specific line and store them.
     */
    private void matchCitation(Path file, int lineNumber, String line) {
        Matcher citeMatch = CITE_PATTERN.matcher(line);

        while (citeMatch.find()) {
            Arrays.stream(citeMatch.group(CITE_GROUP).split(","))
                  .forEach(key -> texParserResult.addKey(key, file, lineNumber, citeMatch.start(), citeMatch.end(), line));
        }
    }

    /**
     * Find inputs and includes along a specific line and store them for parsing later.
     */
    private void matchNestedFile(Path file, List<Path> texFiles, List<Path> referencedFiles, String line) {
        Matcher includeMatch = INCLUDE_PATTERN.matcher(line);

        while (includeMatch.find()) {
            String include = includeMatch.group(INCLUDE_GROUP);

            Path nestedFile = file.getParent().resolve(
                    include.endsWith(TEX_EXT)
                            ? include
                            : String.format("%s%s", include, TEX_EXT));

            if (nestedFile.toFile().exists() && !texFiles.contains(nestedFile)) {
                referencedFiles.add(nestedFile);
            }
        }
    }
}

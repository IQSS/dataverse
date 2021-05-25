package edu.harvard.iq.dataverse.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class ExternalRarDataUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExternalRarDataUtil.class);

    private String utilPath;
    private String utilOpts;
    private String lineDelimiter;

    // -------------------- CONSTRUCTORS --------------------

    public ExternalRarDataUtil(String utilPath, String utilOpts, String lineDelimiter) {
        this.utilPath = utilPath;
        this.utilOpts = utilOpts;
        this.lineDelimiter = lineDelimiter;
    }

    // -------------------- LOGIC --------------------

    public long checkRarExternally(Path tempFile, String fileName) {
        long size = 0L;
        if (StringUtils.isBlank(utilPath)) {
            return size;
        }
        File output = null;
        try {
            output = Files.createTempFile("rar-util-output", "temp").toFile();
            new ProcessBuilder(utilPath, utilOpts, tempFile.toString())
                    .redirectOutput(output)
                    .start();
            try (BufferedReader reader = new BufferedReader(new FileReader(output))) {
                size = parseOutput(reader.lines().toArray(String[]::new));
            }
        } catch (IOException ioe) {
            fileName = StringUtils.isBlank(fileName) ? tempFile.getFileName().toString() : fileName;
            logger.warn("Exception during processing file: " + fileName, ioe);
        } finally {
            if (output != null) {
                output.delete();
            }
        }
        return size;
    }

    long parseOutput(String[] contents) {
        // Output has to have at least two lines
        if (contents.length < 2) {
            return 0L;
        }

        // The total size is at the bottom of the table, right after
        // the line containing the chosen delimiter.
        // So we start checking from the last line of the file.
        int lastLineIndex = contents.length - 1;

        // Set index to last line
        int index = lastLineIndex;

        // Decrement index until we find the last line starting with delimiter
        while (true) {
            String line = contents[index];
            if ((line != null && line.startsWith(lineDelimiter))) {
                break;
            }
            index--;
            if (index < 0) {
                break;
            }
        }

        // Proceed if we've found the line with delimiter in proper place
        if (index >= 0 && index < lastLineIndex) {
            // The line with total size is the next line
            String lineWithSize = contents[++index];

            // Extract first number from the line
            Scanner scanner = new Scanner(lineWithSize);
            return scanner.hasNextLong() ? scanner.nextLong() : 0L;
        }
        return 0L;
    }
}

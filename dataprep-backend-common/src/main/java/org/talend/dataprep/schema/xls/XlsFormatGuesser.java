package org.talend.dataprep.schema.xls;

import java.io.IOException;
import java.util.Collections;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.schema.FormatGuesser;
import org.talend.dataprep.schema.SchemaParser;
import org.talend.dataprep.schema.unsupported.UnsupportedFormatGuess;

@Component("formatGuesser#xls")
public class XlsFormatGuesser implements FormatGuesser {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlsFormatGuesser.class);

    @Autowired
    private XlsFormatGuess xlsFormatGuess;

    /** The fallback guess if the input is not Excel compliant. */
    @Autowired
    private UnsupportedFormatGuess fallbackGuess;

    @Override
    public FormatGuesser.Result guess(SchemaParser.Request request, String encoding) {
        if (request == null || request.getContent() == null) {
            throw new IllegalArgumentException("Content cannot be null.");
        }
        try {
            Workbook workbook = XlsUtils.getWorkbook(request);
            // if poi can read it we assume it's correct excel file && at least one sheet
            if (workbook != null && workbook.getNumberOfSheets() > 0) {
                return new FormatGuesser.Result(xlsFormatGuess, encoding, Collections.emptyMap());
            }
        } catch (IOException e) {
            LOGGER.debug("fail to read content: " + e.getMessage(), e);
        }
        return new FormatGuesser.Result(fallbackGuess, "UTF-8", Collections.emptyMap());
    }
}
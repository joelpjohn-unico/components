package org.talend.components.snowflake.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.talend.components.api.exception.ComponentException;


/**
 * Contains only runtime helper classes, mainly to do with logging.
 */
public class SnowflakeRuntimeHelper {

    private SnowflakeRuntimeHelper() {
    }

    public static StringBuilder addLog(Error[] resultErrors, String row_key, BufferedWriter logWriter) {
        StringBuilder errors = new StringBuilder("");
        if (resultErrors != null) {
            for (Error error : resultErrors) {
                errors.append(error.getMessage()).append("\n");
                if (logWriter != null) {
                    try {
/*                        logWriter.append("\tStatus Code: ").append(error.getStatusCode().toString());
                        logWriter.newLine();
                        logWriter.newLine();
                        logWriter.append("\tRowKey/RowNo: " + row_key);
                        if (error.getFields() != null) {
                            logWriter.newLine();
                            logWriter.append("\tFields: ");
                            boolean flag = false;
                            for (String field : error.getFields()) {
                                if (flag) {
                                    logWriter.append(", ");
                                } else {
                                    flag = true;
                                }
                                logWriter.append(field);
                            }
                        }
                        logWriter.newLine();*/
                        logWriter.newLine();

                        logWriter.append("\tMessage: ").append(error.getMessage());

                        logWriter.newLine();

                        logWriter.append("\t--------------------------------------------------------------------------------");

                        logWriter.newLine();
                        logWriter.newLine();
                    } catch (IOException ex) {
                        ComponentException.unexpectedException(ex);
                    }
                }
            }
        }
        return errors;
    }

    public static Calendar convertDateToCalendar(Date date) {
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            cal.setTime(date);
            return cal;
        } else {
            return null;
        }
    }

}

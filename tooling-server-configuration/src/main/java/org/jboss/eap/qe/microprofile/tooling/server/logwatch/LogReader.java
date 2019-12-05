package org.jboss.eap.qe.microprofile.tooling.server.logwatch;

import java.util.regex.Pattern;

/**
 * Interface describing log watcher which checks log for a pattern.
 */
public interface LogReader {

    /**
     * Perform search in log or its excerpt and find if a line that matches the pattern.
     * @param pattern a pattern which will be the log line matched against
     * @return true if log line matching pattern was found in log, false otherwise
     */
    boolean wasLineLogged(final Pattern pattern);

    /**
     * Perform search in log or its excerpt and find if a line contains a sub string.
     * @param subString a sub string which will be used to perform search among lines
     * @return true if log line containing a sub string was found in log, false otherwise
     */
    boolean wasLineLogged(final String subString);

}

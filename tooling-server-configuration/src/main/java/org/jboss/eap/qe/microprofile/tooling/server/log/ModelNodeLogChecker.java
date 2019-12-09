package org.jboss.eap.qe.microprofile.tooling.server.log;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;

/**
 * Log checker based on accessing messages through {@link org.jboss.dmr.ModelNode} returned from {@code :read-log-file}
 * operation.
 */
public final class ModelNodeLogChecker implements LogChecker {

    private static final String READ_LOG_FILE_OPERATION = "read-log-file";

    private static final Address DEFAULT_STANDALONE_LOG_ADDRESS = Address.subsystem("logging")
            .and("log-file", "server.log");

    private final OnlineManagementClient client;
    private final int countOfLines;
    private final boolean readLogFromEnd;

    /**
     * Create an instance of log checker. File will be read from the end (tail).
     * 
     * @param client client which will be used to invoke log file reading operation on server
     * @param countOfLines number of lines which will be read. -1 means all lines will be read. Be careful, that log
     *        files can get pretty big and reading whole file may lead to serious memory issues.
     */
    public ModelNodeLogChecker(final OnlineManagementClient client, final int countOfLines) {
        this(client, countOfLines, true);
    }

    /**
     * Create an instance of log checker
     * 
     * @param client client which will be used to invoke log file reading operation on server
     * @param countOfLines number of lines which will be read. -1 means all lines will be read. Be careful, that log
     *        files can get pretty big and reading whole file may lead to serious memory issues.
     * @param readFromEnd true if log file should be read in tail mode
     */
    public ModelNodeLogChecker(final OnlineManagementClient client, final int countOfLines, final boolean readFromEnd) {
        this.client = client;
        this.countOfLines = countOfLines;
        this.readLogFromEnd = readFromEnd;
    }

    @Override
    public boolean logMatches(Pattern pattern) {
        try {
            return anyLineMatchesPredicate(readLogFileFromManagementModel(),
                    (final String line) -> pattern.matcher(line).matches());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean logContains(String subString) {
        try {
            return anyLineMatchesPredicate(readLogFileFromManagementModel(),
                    (final String line) -> line.contains(subString));

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModelNode readLogFileFromManagementModel() throws IOException {
        final Operations ops = new Operations(this.client);

        final ModelNodeResult result = ops.invoke(READ_LOG_FILE_OPERATION, DEFAULT_STANDALONE_LOG_ADDRESS,
                Values.of("lines", this.countOfLines)
                        .and("tail", this.readLogFromEnd));

        result.assertSuccess("Reading log file failed!");

        return result.value();
    }

    private boolean anyLineMatchesPredicate(final ModelNode logLines, final Predicate<String> predicate) {
        final Optional<String> optionalMatch = logLines.asList()
                .stream()
                .map(ModelNode::asString)
                .filter(predicate)
                .findFirst();

        return optionalMatch.isPresent();
    }

}

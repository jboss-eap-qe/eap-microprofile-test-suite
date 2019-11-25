package org.jboss.eap.qe.microprofile.common.utilities;

import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic tailer log listener, base on waiting on pattern to appear.
 */
@JBossLog
public final class LogListener implements TailerListener {
    private final AtomicBoolean found;

    private final Pattern pattern;

    private Tailer tailer;

    /**
     * Create new log listener
     * @param pattern pattern which will be waited for to appear in log
     */
    public LogListener(final Pattern pattern) {
        this.found = new AtomicBoolean();
        this.pattern = pattern;
    }

    @Override
    public void init(Tailer tailer) {
        this.tailer = tailer;
    }

    @Override
    public void fileNotFound() {
        log.error(String.format("File '%s' for tailer wasn't found!", tailer.getFile().getAbsolutePath()));
    }

    @Override
    public void fileRotated() {
        log.warn(String.format("File '%s' for tailer was rotated!", tailer.getFile().getAbsolutePath()));
    }

    @Override
    public void handle(String line) {
        final Matcher matcher = this.pattern.matcher(line);
        if (matcher.matches()) {
            this.found.set(true);
        }
    }

    @Override
    public void handle(Exception e) {
        log.error("An exception occurred!", e);
    }

    public AtomicBoolean getFound() {
        return found;
    }
}

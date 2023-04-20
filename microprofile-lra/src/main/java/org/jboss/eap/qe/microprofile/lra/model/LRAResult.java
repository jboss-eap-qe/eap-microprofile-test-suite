package org.jboss.eap.qe.microprofile.lra.model;

import java.net.URI;

public class LRAResult {

    private URI lraId;
    private URI recoveryId;
    private boolean completed;
    private URI finishLraId;
    private URI finishRecoveryId;
    private URI afterLraId;

    public LRAResult() {
    }

    public LRAResult(URI lraId, URI recoveryId, boolean completed, URI finishLraId, URI finishRecoveryId, URI afterLraId) {
        this.lraId = lraId;
        this.recoveryId = recoveryId;
        this.completed = completed;
        this.finishLraId = finishLraId;
        this.finishRecoveryId = finishRecoveryId;
        this.afterLraId = afterLraId;
    }

    @Override
    public String toString() {
        return "LRAResult{" +
                "lraId=" + lraId +
                ", recoveryId=" + recoveryId +
                ", completed=" + completed +
                ", finishLraId=" + finishLraId +
                ", finishRecoveryId=" + finishRecoveryId +
                ", afterLraId=" + afterLraId +
                '}';
    }

    public URI getLraId() {
        return lraId;
    }

    public void setLraId(URI lraId) {
        this.lraId = lraId;
    }

    public URI getRecoveryId() {
        return recoveryId;
    }

    public void setRecoveryId(URI recoveryId) {
        this.recoveryId = recoveryId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public URI getFinishLraId() {
        return finishLraId;
    }

    public void setFinishLraId(URI finishLraId) {
        this.finishLraId = finishLraId;
    }

    public URI getFinishRecoveryId() {
        return finishRecoveryId;
    }

    public void setFinishRecoveryId(URI finishRecoveryId) {
        this.finishRecoveryId = finishRecoveryId;
    }

    public URI getAfterLraId() {
        return afterLraId;
    }

    public void setAfterLraId(URI afterLraId) {
        this.afterLraId = afterLraId;
    }
}

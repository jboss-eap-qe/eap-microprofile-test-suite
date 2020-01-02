package org.jboss.eap.qe.microprofile.tooling.performance.memory;

import org.jboss.eap.qe.microprofile.tooling.performance.core.MeasurementRecord;

/**
 * Represents a record of data resulting from a memory footprint probe.
 */
public class MemoryUsageRecord implements MeasurementRecord<Long> {

    private Long edenGenSpaceUsed;
    private Long metaSpaceUsed;
    private Long oldGenSpaceUsed;
    private Long survivorSpaceUsed;

    public MemoryUsageRecord() {
        this(0L, 0L, 0L, 0L);
    }

    public MemoryUsageRecord(Long edenGenSpaceUsed, Long metaSpaceUsed, Long oldGenSpaceUsed, Long surevivorSpace) {
        this.edenGenSpaceUsed = edenGenSpaceUsed;
        this.metaSpaceUsed = metaSpaceUsed;
        this.oldGenSpaceUsed = oldGenSpaceUsed;
        this.survivorSpaceUsed = surevivorSpace;
    }

    public Long getEdenGenSpaceUsed() {
        return edenGenSpaceUsed;
    }

    public void setEdenGenSpaceUsed(Long edenGenSpaceUsed) {
        this.edenGenSpaceUsed = edenGenSpaceUsed;
    }

    public MemoryUsageRecord edenGenSpaceUsed(Long edenGenSpaceUsed) {
        this.edenGenSpaceUsed = edenGenSpaceUsed;
        return this;
    }

    public Long getMetaSpaceUsed() {
        return metaSpaceUsed;
    }

    public void setMetaSpaceUsed(Long metaSpaceUsed) {
        this.metaSpaceUsed = metaSpaceUsed;
    }

    public MemoryUsageRecord metaSpaceUsed(Long metaSpaceUsed) {
        this.metaSpaceUsed = metaSpaceUsed;
        return this;
    }

    public Long getOldGenSpaceUsed() {
        return oldGenSpaceUsed;
    }

    public void setOldGenSpaceUsed(Long oldGenSpaceUsed) {
        this.oldGenSpaceUsed = oldGenSpaceUsed;
    }

    public MemoryUsageRecord oldGenSpaceUsed(Long oldGenSpaceUsed) {
        this.oldGenSpaceUsed = oldGenSpaceUsed;
        return this;
    }

    public Long getSurvivorSpaceUsed() {
        return survivorSpaceUsed;
    }

    public void setSurvivorSpaceUsed(Long survivorSpaceUsed) {
        this.survivorSpaceUsed = survivorSpaceUsed;
    }

    public MemoryUsageRecord survivorSpaceUsed(Long survivorSpaceUsed) {
        this.survivorSpaceUsed = survivorSpaceUsed;
        return this;
    }

    public Long getHeapSpaceUsed() {
        return edenGenSpaceUsed + oldGenSpaceUsed + survivorSpaceUsed;
    }

    public Long getTotalSpaceUsed() {
        return edenGenSpaceUsed + oldGenSpaceUsed + survivorSpaceUsed + metaSpaceUsed;
    }
}

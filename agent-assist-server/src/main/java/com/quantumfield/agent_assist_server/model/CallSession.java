package com.quantumfield.agent_assist_server.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class CallSession {

    private String contactId;
    private Instant startedAt;
    private String agentId;
    private final Map<String, TranscriptSegment> segmentIndex = new ConcurrentHashMap<>();
    private final List<TranscriptSegment> segments = new CopyOnWriteArrayList<>();

    public boolean hasSegment(String segmentId){
        return segmentIndex.containsKey(segmentId);
    }

    public TranscriptSegment findSegment(String segmentId) {
        return segmentIndex.get(segmentId);
    }

    public void addSegment(TranscriptSegment segment){
        segmentIndex.put(segment.getSegmentId(), segment);
        segments.add(segment); //what if the segments arrive out of order?
    }

    public void updateSegment(String segmentId, TranscriptSegment updated){
        TranscriptSegment existing = segmentIndex.put(segmentId, updated);
        int idx = existing == null ? -1 : segments.indexOf(existing);
        if (idx >= 0) {
            segments.set(idx, updated);
        } else {
            segments.add(updated);
        }
    }
}

package com.quantumfield.agent_assist_server.service;

import com.quantumfield.agent_assist_server.contactlens.TranscriptSegmentRaw;
import com.quantumfield.agent_assist_server.contactlens.UtteranceSegment;
import com.quantumfield.agent_assist_server.model.CallSession;
import com.quantumfield.agent_assist_server.model.TranscriptSegment;

import java.time.Instant;

public class TranscriptIngestService {

    public void storeUtteranceSegment(CallSession callSession, UtteranceSegment segment){
        TranscriptSegment transcriptSegment = new TranscriptSegment();

        transcriptSegment.setPartial(true);
        transcriptSegment.setSegmentId(segment.getTranscriptId());
        transcriptSegment.setText(segment.getPartialContent());
        transcriptSegment.setBeginTime(Instant.ofEpochMilli(segment.getBeginOffsetMillis()));
        transcriptSegment.setEndTime(null);
        transcriptSegment.setSentiment(null);

        if(callSession.hasSegment(transcriptSegment.getSegmentId()))
            callSession.updateSegment(transcriptSegment.getSegmentId() ,transcriptSegment);
        else
            callSession.addSegment(transcriptSegment);
    }

    public void storeTranscriptSegment(CallSession callSession, TranscriptSegmentRaw segment){
        TranscriptSegment transcriptSegment = new TranscriptSegment();

        transcriptSegment.setPartial(false);
        transcriptSegment.setSegmentId(segment.getId());
        transcriptSegment.setText(segment.getContent());
        transcriptSegment.setBeginTime(Instant.ofEpochMilli(segment.getBeginOffsetMillis()));
        transcriptSegment.setEndTime(Instant.ofEpochMilli(segment.getEndOffsetMillis()));
        transcriptSegment.setSentiment(segment.getSentiment());

        if(callSession.hasSegment(transcriptSegment.getSegmentId()))
            callSession.updateSegment(transcriptSegment.getSegmentId() ,transcriptSegment);
        else
            callSession.addSegment(transcriptSegment);
    }
}

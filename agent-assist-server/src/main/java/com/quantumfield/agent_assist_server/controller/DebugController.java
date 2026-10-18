package com.quantumfield.agent_assist_server.controller;

import com.quantumfield.agent_assist_server.websocket.SocketRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class DebugController {
    private final SocketRegistry socketRegistry;

    @PostMapping("/debug/push/{agentId}")
    public ResponseEntity<String> push(@PathVariable("agentId") String agentId,
                                       @RequestBody String body) {
        Optional<WebSocketSession> socket = socketRegistry.findSocket(agentId);
        if (socket.isEmpty()) {
            return ResponseEntity.status(404).body("No open socket for " + agentId);
        }
        WebSocketSession session = socket.get();
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(body));
            }
            return ResponseEntity.ok("sent");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("send failed: " + e.getMessage());
        }
    }
}
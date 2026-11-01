package com.quantumfield.agent_assist_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentAssistServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentAssistServerApplication.class, args);
	}

}

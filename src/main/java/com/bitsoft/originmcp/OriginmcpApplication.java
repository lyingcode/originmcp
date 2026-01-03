package com.bitsoft.originmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import com.bitsoft.originmcp.service.WeatherService;

@SpringBootApplication
public class OriginmcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(OriginmcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider toolCallbackProvider(WeatherService weatherService) {
		return MethodToolCallbackProvider.builder()
			.toolObjects(weatherService)
			.build();
	}

}

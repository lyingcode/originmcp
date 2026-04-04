package com.bitsoft.originmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.tool.ToolCallbackProvider;
import com.bitsoft.originmcp.dynamicregistry.DynamicToolCallbackProvider;
import com.bitsoft.originmcp.dynamicregistry.DynamicToolRegistry;
import com.bitsoft.originmcp.dynamicregistry.ToolInvoker;

@SpringBootApplication
public class OriginmcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(OriginmcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider toolCallbackProvider(
			DynamicToolRegistry dynamicToolRegistry,
			DynamicToolCallbackProvider dynamicCallbackProvider,
			ToolInvoker toolInvoker) {
		return dynamicCallbackProvider;
	}

}

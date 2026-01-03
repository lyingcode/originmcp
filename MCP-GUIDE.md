# Weather MCP Server - 团队通用 MCP Server 开发指南

## 📋 项目概述

这是一个基于 **Spring AI MCP Server** 开发的天气查询服务,展示如何开发团队通用的 MCP Server,供 AI 助手(Claude Desktop、Cursor 等)调用。

## 🎯 MCP Server 核心概念

### 什么是 MCP (Model Context Protocol)?

MCP 是 Anthropic 推出的开放协议,用于 AI 应用与外部工具/数据源的标准化集成。

**关键特性**:
- 📡 **标准化通信**: 使用 JSON-RPC 2.0 协议
- 🔧 **工具暴露**: 通过 `@Tool` 注解将方法暴露给 AI
- 🔌 **传输方式**: 支持 stdio (标准输入输出) 和 SSE (Server-Sent Events)
- 🤖 **AI 集成**: Claude Desktop、Cursor 等可直接调用

## 🏗️ 项目结构

```
originmcp/
├── src/main/java/com/bitsoft/originmcp/
│   ├── service/
│   │   └── WeatherService.java          # MCP 工具实现 (@Tool)
│   ├── controller/
│   │   └── WeatherController.java       # REST API (可选)
│   └── OriginmcpApplication.java        # Spring Boot 主类
├── src/main/resources/
│   └── application.properties           # MCP Server 配置
├── pom.xml                              # Maven 依赖
├── claude_desktop_config.example.json   # Claude Desktop 配置示例
└── test-mcp-server.js                   # MCP Server 测试脚本
```

## 🔧 MCP Server 配置

### 1. Maven 依赖 (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
```

### 2. 应用配置 (application.properties)

```properties
spring.application.name=originmcp

# MCP Server Configuration
spring.ai.mcp.server.transport=stdio
spring.ai.mcp.server.name=weather-mcp-server
spring.ai.mcp.server.version=1.0.0
```

**配置说明**:
- `transport=stdio`: 使用标准输入输出通信(适用于 Claude Desktop)
- `name`: MCP Server 名称
- `version`: 版本号

### 3. 暴露 MCP 工具

使用 `@Tool` 注解将方法暴露为 MCP 工具:

```java
@Service
public class WeatherService {

    @Tool(description = "Get current weather for a specific location by city name or coordinates")
    public String getWeather(String location) {
        // 实现逻辑
    }

    @Tool(description = "Get detailed 7-day weather forecast for a location")
    public String getForecast(String location) {
        // 实现逻辑
    }
}
```

**@Tool 注解说明**:
- `description`: 工具描述,AI 会根据此描述决定何时调用
- 方法名: 成为工具名称 (如 `getWeather`)
- 参数: 自动映射为工具参数

## 🚀 部署和使用

### 方式 1: Claude Desktop 集成 (推荐)

#### 步骤 1: 打包项目

```bash
mvn clean package
```

生成的 JAR: `target/originmcp-0.0.1-SNAPSHOT.jar`

#### 步骤 2: 配置 Claude Desktop

1. 找到配置文件:
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

2. 编辑配置文件:

```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": [
        "-jar",
        "D:/projects/originmcp/target/originmcp-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

**重要**: 将路径替换为你的实际 JAR 路径。

#### 步骤 3: 重启 Claude Desktop

重启后,在 Claude 中可以直接使用:

```
深圳现在天气怎么样?
```

```
帮我查一下北京未来一周的天气预报
```

Claude 会自动调用你的 MCP Server 获取数据!

### 方式 2: Cursor 集成

Cursor 的 MCP 配置类似,参考 Cursor 文档配置 MCP Server。

### 方式 3: 自定义 AI 应用集成

如果你在开发自己的 AI 应用,可以通过 MCP 客户端库连接:

```java
// 使用 Spring AI MCP Client
McpClient client = new StdioMcpClient(
    "java",
    List.of("-jar", "path/to/originmcp.jar")
);

// 列出可用工具
List<Tool> tools = client.listTools();

// 调用工具
String result = client.callTool("getWeather", Map.of("location", "beijing"));
```

## 🧪 测试 MCP Server

### 方法 1: 使用测试脚本 (推荐)

```bash
# 1. 确保已安装 Node.js
node --version

# 2. 打包项目
mvn clean package

# 3. 运行测试
node test-mcp-server.js
```

测试脚本会:
1. ✅ 启动 MCP Server
2. ✅ 列出所有可用工具
3. ✅ 调用 `getWeather` 工具查询深圳天气
4. ✅ 显示完整的 MCP 协议交互

### 方法 2: 使用 MCP Inspector (官方工具)

```bash
# 安装 MCP Inspector
npm install -g @modelcontextprotocol/inspector

# 启动检查器
mcp-inspector java -jar target/originmcp-0.0.1-SNAPSHOT.jar
```

### 方法 3: Claude Desktop 直接测试

1. 按照上述步骤配置 Claude Desktop
2. 在 Claude 中询问天气
3. 查看 Claude 是否成功调用你的 MCP Server

## 📊 验证 MCP Server 是否工作

### 检查清单:

- [ ] JAR 文件成功生成 (`target/originmcp-0.0.1-SNAPSHOT.jar`)
- [ ] 配置文件路径正确
- [ ] Claude Desktop 配置正确
- [ ] 重启 Claude Desktop
- [ ] 在 Claude 中询问天气相关问题
- [ ] Claude 能够返回实时天气数据

### 调试技巧:

1. **查看 Claude Desktop 日志**:
   - Windows: `%APPDATA%\Claude\logs`
   - macOS: `~/Library/Logs/Claude`

2. **手动测试 JAR**:
   ```bash
   java -jar target/originmcp-0.0.1-SNAPSHOT.jar
   ```

   然后手动输入 JSON-RPC 请求:
   ```json
   {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
   ```

3. **检查工具是否暴露**:
   - 工具名称应为: `getWeather`, `getForecast`
   - 参数应正确映射

## 🔍 MCP 协议示例

### 列出工具 (tools/list)

**请求**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

**响应**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "getWeather",
        "description": "Get current weather for a specific location by city name or coordinates",
        "inputSchema": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string"
            }
          },
          "required": ["location"]
        }
      },
      {
        "name": "getForecast",
        "description": "Get detailed 7-day weather forecast for a location",
        "inputSchema": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string"
            }
          },
          "required": ["location"]
        }
      }
    ]
  }
}
```

### 调用工具 (tools/call)

**请求**:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "getWeather",
    "arguments": {
      "location": "shenzhen"
    }
  }
}
```

**响应**:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Current Weather for shenzhen:\n\nTemperature: 15.0°C\nHumidity: 52%\nWind Speed: 7.6 km/h\nConditions: Clear sky\n"
      }
    ]
  }
}
```

## 🎓 开发团队通用 MCP Server 的最佳实践

### 1. 工具设计原则

✅ **清晰的描述**: `@Tool(description = "...")`要详细,AI 才能正确调用
✅ **简单的参数**: 参数尽量简单,使用基本类型或 String
✅ **错误处理**: 捕获异常,返回友好的错误信息
✅ **幂等性**: 工具调用应该是幂等的

### 2. 命名规范

- **服务名**: `{domain}-mcp-server` (如 `weather-mcp-server`)
- **工具名**: 使用动词开头 (如 `getWeather`, `createOrder`)
- **参数名**: 使用清晰的语义名称

### 3. 配置管理

```properties
# 开发环境
spring.ai.mcp.server.transport=stdio
spring.profiles.active=dev

# 生产环境
spring.ai.mcp.server.transport=sse
spring.ai.mcp.server.port=8080
```

### 4. 日志和监控

```java
@Tool(description = "...")
public String getWeather(String location) {
    log.info("MCP Tool called: getWeather, location={}", location);
    try {
        String result = fetchWeather(location);
        log.info("MCP Tool success: getWeather");
        return result;
    } catch (Exception e) {
        log.error("MCP Tool error: getWeather", e);
        return "Error: " + e.getMessage();
    }
}
```

## 📚 扩展开发

### 添加新工具

1. 在 Service 中添加方法
2. 添加 `@Tool` 注解
3. 重新打包
4. 重启 Claude Desktop

示例:
```java
@Tool(description = "Search weather by coordinates")
public String searchByCoordinates(double latitude, double longitude) {
    return getWeather(latitude + "," + longitude);
}
```

### 支持多个 MCP Server

在 Claude Desktop 配置中添加多个服务:

```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": ["-jar", "weather-mcp-server.jar"]
    },
    "database": {
      "command": "java",
      "args": ["-jar", "database-mcp-server.jar"]
    }
  }
}
```

## 🔗 相关资源

- **Spring AI 文档**: https://docs.spring.io/spring-ai/reference/
- **MCP 协议规范**: https://spec.modelcontextprotocol.io/
- **Anthropic MCP 文档**: https://docs.anthropic.com/claude/docs/mcp
- **示例项目**: https://github.com/anthropics/anthropic-sdk-java

## ❓ 常见问题

### Q: Claude 没有调用我的 MCP Server?

**排查步骤**:
1. 检查 JAR 路径是否正确
2. 查看 Claude Desktop 日志
3. 确认已重启 Claude Desktop
4. 运行测试脚本验证 MCP Server 是否正常

### Q: 工具没有暴露?

**检查**:
1. 确认方法有 `@Tool` 注解
2. 确认类有 `@Service` 注解
3. 确认方法是 public
4. 重新打包项目

### Q: 如何调试 MCP 协议?

**方法**:
1. 使用 `test-mcp-server.js` 脚本
2. 查看 stdout/stderr 输出
3. 使用 MCP Inspector
4. 添加详细日志

---

**开发团队**: 如有问题,请参考本文档或联系架构组。

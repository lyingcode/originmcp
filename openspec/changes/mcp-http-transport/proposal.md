## Why

当前 MCP Server 使用 STDIO transport，通过标准输入/输出进行 JSON-RPC 通信。这种方式适用于本地 CLI 场景，但无法被标准 HTTP 客户端（如 curl、Postman）直接调用，也不便于通过 API Gateway 进行管理。将 MCP Server 改为 HTTP stateless 模式，对外提供 RESTful API，可以扩大其适用范围，便于与各种 HTTP 客户端集成。

## What Changes

- **Transport 切换**：从 STDIO 改为 HTTP/REST，使用 `@RestController` 提供端点
- **端点路径**：MCP 服务端点改为 `POST /origin/mcp`
- **无状态设计**：每次请求独立处理，不维护会话状态
- **移除 STDIO 配置**：删除 `spring.main.web-application-type=none`，启用嵌入式 web 服务器
- **引入 HTTP 依赖**：需要添加 `spring-boot-starter-web` 依赖

## Capabilities

### New Capabilities
- `mcp-http-endpoint`: MCP Server HTTP 端点，支持 JSON-RPC over HTTP
- `mcp-request-handler`: HTTP 请求处理器，将 JSON-RPC 请求路由到工具调用

### Modified Capabilities
- `mcp-auth`: 认证机制需要适配 HTTP Header 方式（从环境变量改为 HTTP Header）
- `mcp-rate-limiting`: 限流机制需要适配 HTTP Filter 实现

## Impact

- **新增依赖**：`spring-boot-starter-web`
- **配置变更**：
  - 删除 `spring.main.web-application-type=none`
  - 新增 `server.port` 配置
  - 端点路径 `/origin/mcp`
- **安全组件调整**：
  - `McpSecurityFilter` 可以启用（之前因 STDIO 无法使用 Filter）
  - 认证方式从环境变量改为 HTTP Header (`X-API-Key`)

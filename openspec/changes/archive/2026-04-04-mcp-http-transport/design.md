## Context

当前 MCP Server 使用 Spring AI MCP Server Starter，通过 STDIO transport 对外提供服务。系统通过标准输入/输出进行 JSON-RPC 通信。

**当前配置**:
- `spring.main.web-application-type=none` (禁用 Web 服务器)
- MCP 通信通过 `spring.ai.mcp.server.stdio=true` 配置

**问题**:
- STDIO 方式无法被标准 HTTP 客户端调用
- 不便于通过 API Gateway 进行管理
- 无法使用 Spring Security Filter 进行请求拦截

## Goals / Non-Goals

**Goals:**
- 将 MCP Server 从 STDIO 改为 HTTP/REST transport
- 提供无状态的 HTTP 端点 `POST /origin/mcp`
- 启用 Spring Security Filter 进行认证和限流
- 支持标准 JSON-RPC over HTTP

**Non-Goals:**
- 不改变工具注册和动态加载机制
- 不改变工具调用逻辑
- 不移除 STDIO 支持（可选保留）

## Decisions

### 1. HTTP Endpoint 实现方案

**决策**: 使用 `@RestController` 实现 MCP HTTP 端点。

**实现方案**:
- 创建 `McpHttpController` 处理 `POST /origin/mcp` 请求
- 请求 Body 为 JSON-RPC 2.0 格式的 Request
- 响应 Body 为 JSON-RPC 2.0 格式的 Response
- 启用 `spring-boot-starter-web` 依赖

**替代方案**:
- Spring WebFlux (WebFlux): 过于复杂，不需要响应式
- 直接使用 Servlet API: 不如 @RestController 简洁

### 2. Filter 启用方案

**决策**: 启用 `McpSecurityFilter` 进行认证和限流。

**实现方案**:
- 删除 `spring.main.web-application-type=none` 配置
- `McpSecurityFilter` 通过 `FilterRegistrationBean` 注册
- 认证从 HTTP Header `X-API-Key` 获取

**替代方案**:
- HandlerInterceptor: Filter 更适合安全检查，且可处理所有请求

### 3. JSON-RPC 消息处理

**决策**: 复用 Spring AI MCP Server 的 JSON-RPC 解析逻辑。

**实现方案**:
- 使用 Spring AI 的 `JsonRpcRequest` / `JsonRpcResponse` 对象
- 通过 `ObjectMapper` 进行 JSON 序列化/反序列化
- 将请求路由到 `DynamicToolRegistry` 进行工具调用

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| HTTP 端点暴露安全风险 | 启用 API Key 认证和限流 Filter |
| 性能下降（HTTP vs STDIO） | HTTP 启动开销大，但可接受 |
| 客户端兼容性 | 提供标准 JSON-RPC 2.0 接口 |

## Open Questions

1. **STDIO 是否保留**: 是否需要同时保留 STDIO 支持？
2. **服务端口**: 默认使用哪个端口？是否需要可配置？

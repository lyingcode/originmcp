## 1. 依赖和配置变更

- [x] 1.1 添加 `spring-boot-starter-web` 依赖到 pom.xml
- [x] 1.2 修改 application.properties：删除 `spring.main.web-application-type=none`
- [x] 1.3 添加 `server.port` 和端点路径配置
- [x] 1.4 添加 CORS 配置（如果需要跨域访问）

## 2. HTTP 端点实现

- [x] 2.1 创建 `McpHttpController` REST 控制器
- [x] 2.2 实现 `POST /origin/mcp` 端点处理 JSON-RPC 请求
- [x] 2.3 实现 JSON-RPC 错误响应处理（Parse error, Invalid Request 等）
- [x] 2.4 实现工具调用结果序列化为 JSON-RPC 响应

## 3. Filter 启用（之前 STDIO 模式无法使用）

- [x] 3.1 启用 `McpSecurityFilter`：修改 FilterRegistrationBean 移除条件注解
- [x] 3.2 启用 `RateLimiter`：在 Filter 中集成限流检查
- [x] 3.3 启用认证：从 HTTP Header `X-API-Key` 读取 API Key

## 4. 测试和验证

- [ ] 4.1 使用 curl 测试 MCP HTTP 端点
- [ ] 4.2 验证 JSON-RPC 请求/响应格式
- [ ] 4.3 验证认证和限流功能
- [ ] 4.4 验证错误情况处理

## 5. 清理（可选）

- [ ] 5.1 移除 STDIO 相关配置（可选保留）
- [ ] 5.2 清理 schema.sql 中的旧示例数据

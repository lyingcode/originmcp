## Why

当前 MCP Server 使用 STDIO transport 对外提供服务，但缺乏任何安全防护机制（认证、授权、输入校验、限流），存在严重的安全风险。必须为 MCP Server 添加完善的安全层，保护工具调用免受未授权访问和恶意攻击。

## What Changes

- **认证机制**：为 MCP Server 添加 API Key 认证，支持客户端在请求中携带认证信息
- **授权控制**：基于工具粒度的权限控制，可配置不同客户端对不同工具的访问权限
- **输入校验**：在工具调用前进行参数校验，防止注入攻击和非法参数
- **限流保护**：基于客户端 IP 和 API Key 的请求限流，防止 DoS 攻击
- **安全配置**：敏感配置（数据库密码、API Key 等）通过环境变量或加密方式管理

## Capabilities

### New Capabilities
- `mcp-auth`: MCP Server 认证机制，支持 API Key 认证和请求签名验证
- `mcp-authorization`: MCP Server 授权机制，基于工具粒度的访问控制
- `mcp-input-validation`: MCP 工具参数校验，防止恶意输入
- `mcp-rate-limiting`: MCP Server 请求限流，防止资源耗尽
- `secure-config`: 安全配置管理，支持敏感信息加密存储

### Modified Capabilities
<!-- 当前没有现有 spec，暂为空 -->

## Impact

- **新增依赖**：Bucket4j（限流）、Jasypt（配置加密），不引入 Spring Security
- **配置变更**：新增安全相关配置项（认证、限流等）
- **MCP 行为变更**：请求处理链路增加安全检查环节
- **工具注册**：动态工具注册时需同时注册权限信息

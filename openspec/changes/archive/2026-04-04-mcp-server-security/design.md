## Context

当前 MCP Server 使用 Spring AI MCP Server Starter，通过 STDIO transport 对外提供服务。系统架构如下：

- **Transport**: STDIO（标准输入/输出）
- **工具管理**: 动态工具注册（从数据库加载工具定义）
- **安全现状**: 无认证、无授权、无输入校验、无限流保护

数据库中存储工具定义（`mcp_tool_definition`）和服务方法映射，通过反射调用。由于 MCP Server 直接暴露给外部调用者，必须添加安全层。

## Goals / Non-Goals

**Goals:**
- 为 MCP Server 添加 API Key 认证机制
- 实现基于工具粒度的授权控制
- 添加工具参数输入校验，防止注入攻击
- 实现请求限流，防止 DoS 攻击
- 安全管理敏感配置（密码、密钥）

**Non-Goals:**
- 不实现 OAuth2/JWT 等复杂认证机制（当前只需要 API Key）
- 不修改数据库 schema（工具权限信息可扩展现有表或新建关联表）
- 不改变 STDIO transport 方式
- 不涉及工具注册流程的安全（仅运行时调用安全）

## Decisions

### 1. 认证机制：API Key 认证（Filter 实现）

**决策**: 采用 API Key 认证，使用 Spring Boot 原生的 `Filter` 实现，统一处理认证和限流。

**实现方案**:
- 创建 `McpSecurityFilter` 实现 `Filter` 接口，在 doFilter 中校验 API Key
- 通过 `FilterRegistrationBean` 注册 Filter
- API Key 存储在数据库表 `mcp_api_keys` 中，每个 Key 关联 client_id
- 每个 Key 关联权限范围（可访问的工具列表）

**替代方案**:
- Spring Security: 过于重量级，本项目不需要完整的 Security 功能
- OAuth2/JWT: 过于复杂，当前场景只需要简单的 API Key
- HandlerInterceptor: 限流和认证分开实现不如 Filter 统一

### 2. 授权机制：工具级权限矩阵

**决策**: 在 `McpToolDefinition` 表或新表 `mcp_client_permissions` 中维护 `client_id -> tool_names` 的权限映射。

**实现方案**:
- 请求通过认证后，根据 client_id 查找其有权限的工具列表
- `ToolInvoker` 调用前检查目标工具是否在权限列表中
- 未授权调用返回错误码 `ACCESS_DENIED`

### 3. 输入校验：参数 Schema 校验

**决策**: 复用 `McpToolParameter` 表中已定义的参数类型和约束，进行运行时校验。

**实现方案**:
- 在 `ToolInvoker` 调用前，根据工具定义中的参数约束进行校验
- 类型检查、必填检查、格式检查（如正则表达式）
- 校验失败返回错误码 `INVALID_PARAMETERS`

### 4. 限流机制：基于滑动窗口的客户端限流

**决策**: 使用 `Filter` + 滑动窗口算法实现限流，不引入新的第三方组件。

**实现方案**:
- 在 `McpSecurityFilter` 中实现限流逻辑
- 使用 `ConcurrentHashMap` 存储每个客户端的请求时间戳列表
- 滑动窗口：固定时间窗口内限制请求次数
- 默认限制：每分钟 60 次调用
- 触发限流返回错误码 `RATE_LIMITED`

**需要确认的组件**:
- Bucket4j: 功能完整但引入新依赖，待确认是否使用

### 5. 安全配置：环境变量 + 配置加密

**决策**: 敏感配置通过环境变量注入，启动时解密。

**实现方案**:
- 数据库密码、API Key 密文存储
- 使用自定义加密工具类（Base64 + AES）
- 启动时通过环境变量注入密钥，解密配置

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 限流影响正常高并发请求 | 提供可配置的限流阈值，允许白名单 |
| API Key 泄露风险 | 定期轮换 Key，支持 Key 过期 |
| 参数校验影响性能 | 使用高效的正则表达式匹配，合理缓存 |
| 兼容性：现有客户端需要升级 | 提供向后兼容的错误码，支持新旧客户端共存 |

## Open Questions

1. **权限配置存储**: 新建 `mcp_client_permissions` 表还是扩展现有 `mcp_tool_definition` 表？
2. **限流粒度**: 按 IP、API Key 还是两者结合？
3. **配置加密方案**: 使用 Jasypt 还是自定义 Base64+AES 加密？
4. **限流组件**: 使用滑动窗口算法自实现，还是引入 Bucket4j（待确认）？

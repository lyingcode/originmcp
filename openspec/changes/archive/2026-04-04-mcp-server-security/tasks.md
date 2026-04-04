## 1. 基础设施准备

- [x] 1.1 创建数据库表 `mcp_api_keys` 存储 API Key 和客户端信息
- [x] 1.2 创建数据库表 `mcp_client_permissions` 存储客户端-工具权限映射
- [x] 1.3 添加安全相关配置项到 application.properties
- [x] 1.4 自实现滑动窗口限流（基于 ConcurrentHashMap）
- [x] 1.5 自实现 Base64+AES 加密工具类

## 2. 认证功能实现（STDIO 适配）

- [x] 2.1 创建 `McpApiKey` 实体类和 `McpApiKeyMapper`
- [x] 2.2 创建 `McpAuthenticator` 从环境变量/配置读取 API Key 进行校验
- [x] 2.3 屏蔽日志中的 API Key 输出

## 3. 授权功能实现

- [x] 3.1 创建 `McpClientPermission` 实体类和 `McpClientPermissionMapper`
- [x] 3.2 创建 `McpAuthorizationService` 实现权限检查逻辑
- [x] 3.3 在 `ToolSecurityInterceptor` 中集成权限检查
- [x] 3.4 实现默认拒绝策略（无显式权限则拒绝）

## 4. 输入校验功能实现

- [x] 4.1 创建 `ParameterValidator` 工具类实现类型和必填检查
- [x] 4.2 创建 `FormatValidator` 实现正则格式校验（集成在 ParameterValidator 中）
- [x] 4.3 在 `ToolSecurityInterceptor` 中集成参数校验
- [x] 4.4 返回结构化的参数校验错误信息

## 5. 限流功能实现

- [x] 5.1 创建 `RateLimiter` 实现滑动窗口限流
- [x] 5.2 使用 `ConcurrentHashMap` 存储客户端请求时间戳
- [x] 5.3 支持可配置的限流阈值（默认 60次/分钟）

## 6. 安全配置实现

- [x] 6.1 创建 `AesEncryptor` 工具类实现 Base64+AES 加密
- [x] 6.2 修改数据库密码和敏感配置从环境变量读取
- [x] 6.3 添加启动时配置解密初始化（SecurityInitializer）

# Weather MCP Server - 团队通用 MCP 服务

## 🎯 项目简介

这是一个基于 Spring AI 开发的 MCP (Model Context Protocol) Server 示例项目,用于验证 MCP 开发流程,供团队开发通用 MCP Server 参考。

**核心功能**:
- ✅ 提供天气查询 MCP 工具
- ✅ 支持 Cursor/VSCode/Claude Code CLI 调用
- ✅ 完整的测试和验证工具
- ✅ 团队开发规范参考

## 📦 快速开始

### 1. 打包项目

```bash
mvn clean package
```

### 2. 验证 MCP Server

#### 方法一: 使用测试脚本 (推荐)

```bash
node test-mcp-server.js
```

**预期输出**:
```
🚀 MCP Server 测试工具
===========================================
JAR 路径: d:\projects\originmcp\target\originmcp-0.0.1-SNAPSHOT.jar
===========================================

⏳ 正在启动 MCP Server...
   (这可能需要几秒钟)

📤 发送请求: tools/list

✅ 收到 MCP 响应:
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "getWeather",
        "description": "Get current weather and forecast for a specific location by city name or coordinates"
      },
      {
        "name": "getForecast",
        "description": "Get detailed 7-day weather forecast for a location"
      }
    ]
  }
}

📋 可用工具列表:
  - getWeather: Get current weather and forecast for a specific location by city name or coordinates
  - getForecast: Get detailed 7-day weather forecast for a location

✅ 测试通过: 工具数量正确 (2个)

📤 发送请求: tools/call (getWeather, location=shenzhen)

🌤️  天气查询结果:
Current Weather for shenzhen:

Temperature: 15.0°C
Humidity: 52%
Wind Speed: 7.6 km/h
Conditions: Clear sky

✅ 测试通过: 成功获取天气数据

===========================================
📊 测试总结
===========================================
✅ 通过: 2
❌ 失败: 0
📝 总计: 2

🎉 所有测试通过!MCP Server 工作正常!
```

#### 方法二: Cursor 验证

1. 项目根目录已包含 `.cursor/mcp.json` 配置
2. 重启 Cursor
3. 在 AI Chat 中询问: "深圳现在天气怎么样?"
4. Cursor 应自动调用 `getWeather` 工具

#### 方法三: VSCode (Continue.dev) 验证

参考 [MCP-VERIFICATION.md](./MCP-VERIFICATION.md) 进行配置。

### 3. 可用的 MCP 工具

| 工具名 | 描述 | 参数 | 示例 |
|--------|------|------|------|
| `getWeather` | 获取当前天气 | location (城市名/坐标) | "深圳现在天气怎么样?" |
| `getForecast` | 获取7天预报 | location (城市名/坐标) | "北京未来一周天气如何?" |

**支持的位置**:
- 中文: 北京、上海、深圳、广州
- 英文: beijing, shanghai, shenzhen, guangzhou, new york, london, tokyo, paris
- 坐标: `22.5431,114.0579` (纬度,经度)

## 📁 项目结构

```
originmcp/
├── src/main/java/
│   └── com/bitsoft/originmcp/
│       ├── service/
│       │   └── WeatherService.java       # MCP 工具实现 (@Tool)
│       ├── controller/
│       │   └── WeatherController.java    # REST API (可选)
│       └── OriginmcpApplication.java
├── src/main/resources/
│   └── application.properties            # MCP 配置
├── .cursor/
│   └── mcp.json                          # Cursor MCP 配置
├── test-mcp-server.js                    # MCP 测试脚本
├── claude_desktop_config.example.json    # Claude Desktop 配置示例
├── MCP-VERIFICATION.md                   # 详细验证指南
├── MCP-GUIDE.md                          # 完整开发指南
└── README.md                             # 本文件
```

## 🔧 技术栈

- **Spring Boot 3.5.9** - 应用框架
- **Spring AI 1.0.0-SNAPSHOT** - MCP Server 支持
- **Java 21** - 编程语言
- **Open-Meteo API** - 天气数据源(免费,无需 API Key)
- **Maven** - 构建工具

## 📖 文档导航

| 文档 | 用途 |
|------|------|
| [README.md](./README.md) | 快速入门(本文件) |
| [MCP-VERIFICATION.md](./MCP-VERIFICATION.md) | **验证 MCP Server 的详细步骤** ⭐ |
| [MCP-GUIDE.md](./MCP-GUIDE.md) | 完整的 MCP 开发指南 |
| [QUICKSTART.md](./QUICKSTART.md) | REST API 使用(可选) |

**推荐阅读顺序**:
1. 本文件 (快速开始)
2. [MCP-VERIFICATION.md](./MCP-VERIFICATION.md) (验证 MCP)
3. [MCP-GUIDE.md](./MCP-GUIDE.md) (深入开发)

## ✅ 验证检查清单

### 开发阶段:
- [ ] 项目能够成功编译: `mvn clean package`
- [ ] JAR 文件生成: `target/originmcp-0.0.1-SNAPSHOT.jar`
- [ ] 测试脚本通过: `node test-mcp-server.js`
- [ ] 工具列表正确: 包含 `getWeather` 和 `getForecast`
- [ ] 工具调用成功: 返回实时天气数据

### 集成阶段:
- [ ] Cursor MCP 配置完成
- [ ] Cursor 能识别 MCP Server
- [ ] 在 Cursor 中询问天气能触发工具调用
- [ ] 返回的数据格式正确

### 团队推广:
- [ ] 文档完整且易懂
- [ ] 团队成员能够成功配置
- [ ] 有明确的故障排查指南

## 🐛 常见问题

### Q1: 测试脚本失败?

**检查**:
```bash
# 1. 确认 Java 版本
java -version  # 应该是 Java 21

# 2. 确认 Node.js 已安装
node --version

# 3. 确认 JAR 存在
dir target\originmcp-0.0.1-SNAPSHOT.jar  # Windows
ls target/originmcp-0.0.1-SNAPSHOT.jar   # Linux/Mac

# 4. 手动运行 JAR 查看错误
java -jar target\originmcp-0.0.1-SNAPSHOT.jar
```

### Q2: Cursor 不识别 MCP Server?

**解决**:
1. 确认 `.cursor/mcp.json` 中的路径是**绝对路径**
2. 检查 Java 在 PATH 中: `java -version`
3. 重启 Cursor
4. 查看 Cursor 设置中的 MCP 配置

### Q3: 工具调用失败?

**调试**:
1. 运行测试脚本确认 MCP Server 正常
2. 检查日志: `logs/mcp-server.log`
3. 验证参数格式是否正确
4. 测试 REST API 是否正常

## 🚀 下一步

### 对于验证 MCP:
1. ✅ 运行测试脚本
2. ✅ 配置 Cursor
3. ✅ 在 Cursor 中测试

### 对于团队开发:
1. 📖 阅读 [MCP-GUIDE.md](./MCP-GUIDE.md)
2. 🔧 参考 WeatherService 实现自己的工具
3. 📝 建立团队 MCP Server 开发规范
4. 🎯 开发通用的业务 MCP 工具

## 💡 开发建议

### 1. 工具设计原则

```java
@Tool(description = "简洁清晰的工具描述,AI 会根据此决定何时调用")
public String toolName(String param) {
    // 1. 参数验证
    // 2. 业务逻辑
    // 3. 错误处理
    // 4. 返回格式化结果
}
```

### 2. 命名规范

- **服务名**: `{domain}-mcp-server`
- **工具名**: 动词开头 (get/create/update/delete/search)
- **参数名**: 清晰的语义

### 3. 错误处理

```java
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("Tool error", e);
    return "Error: " + e.getMessage();
}
```

## 🔗 相关资源

- **Spring AI 文档**: https://docs.spring.io/spring-ai/reference/
- **MCP 协议规范**: https://spec.modelcontextprotocol.io/
- **Open-Meteo API**: https://open-meteo.com/
- **Cursor 文档**: https://cursor.sh/docs

## 📞 支持

如有问题:
1. 查看 [MCP-VERIFICATION.md](./MCP-VERIFICATION.md) 故障排查部分
2. 运行 `node test-mcp-server.js` 查看详细错误
3. 联系项目维护者

---

**最后更新**: 2026-01-03
**项目状态**: ✅ 可用于团队验证和参考

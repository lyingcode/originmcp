# MCP Server 验证指南 - Cursor/VSCode/Claude Code CLI

## 🎯 目标

验证开发的 MCP Server 是否正确暴露工具,供团队 AI 助手调用。

## 📦 准备工作

### 1. 打包 MCP Server

```bash
cd d:\projects\originmcp
mvn clean package
```

确认生成: `target\originmcp-0.0.1-SNAPSHOT.jar`

## 🔧 验证方式

### 方式 1: Cursor 验证 (推荐)

#### 步骤 1: 配置 Cursor MCP

1. 在项目根目录创建 `.cursor/mcp.json`:

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

**注意**: 使用绝对路径!

2. 重启 Cursor

#### 步骤 2: 在 Cursor 中验证

打开 Cursor AI Chat,询问:

```
深圳现在天气怎么样?
```

或者:

```
帮我查询北京未来一周的天气预报
```

#### 步骤 3: 查看 MCP 工具是否被调用

Cursor 应该会:
1. 识别到 `weather` MCP Server
2. 列出可用工具: `getWeather`, `getForecast`
3. 调用对应工具
4. 返回实时天气数据

**如何确认 MCP 正在工作?**

在 Cursor 中,你应该能看到类似的提示:
```
Using tool: getWeather(location="shenzhen")
```

### 方式 2: VSCode 验证

#### 步骤 1: 安装 MCP 扩展

在 VSCode 中安装以下扩展之一:
- **Claude Dev** (如果可用)
- **Continue.dev** (支持 MCP)

#### 步骤 2: 配置 MCP

**Continue.dev 配置** (`.continue/config.json`):

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

#### 步骤 3: 重启 VSCode 并测试

在 Continue 侧边栏询问天气相关问题。

### 方式 3: Claude Code CLI 验证 (命令行)

#### 步骤 1: 安装 Claude Code CLI

```bash
npm install -g @anthropic-ai/claude-code
```

或使用 brew (macOS):
```bash
brew install claude-code
```

#### 步骤 2: 创建 MCP 配置

创建配置文件 `~/.config/claude-code/config.json`:

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

#### 步骤 3: 使用 CLI 验证

```bash
# 启动 Claude Code CLI
claude-code

# 在 CLI 中询问
> 深圳现在天气怎么样?

# 或者直接执行
claude-code "查询北京未来一周天气"
```

### 方式 4: 手动测试 MCP 协议 (底层验证)

如果上述方式都不行,可以手动测试 MCP 协议:

#### 创建测试脚本 `test-mcp.ps1` (Windows PowerShell):

```powershell
# 启动 MCP Server
$process = Start-Process java -ArgumentList "-jar", "target\originmcp-0.0.1-SNAPSHOT.jar" -NoNewWindow -PassThru -RedirectStandardInput "mcp-input.txt" -RedirectStandardOutput "mcp-output.txt"

# 等待启动
Start-Sleep -Seconds 2

# 发送 tools/list 请求
@"
{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
"@ | Out-File -FilePath "mcp-input.txt" -Encoding UTF8

# 等待响应
Start-Sleep -Seconds 2

# 读取响应
Get-Content "mcp-output.txt"

# 清理
Stop-Process -Id $process.Id
```

运行:
```bash
powershell -File test-mcp.ps1
```

#### 或使用 Node.js 测试 (已提供):

```bash
node test-mcp-server.js
```

这个脚本会:
1. ✅ 启动 MCP Server
2. ✅ 列出所有工具
3. ✅ 调用 getWeather 工具
4. ✅ 显示完整响应

## 📊 验证检查清单

### MCP Server 正常工作的标志:

- [ ] JAR 文件成功生成
- [ ] MCP Server 可以启动(不报错)
- [ ] 配置文件路径正确
- [ ] AI 助手能识别 MCP Server
- [ ] 工具列表包含 `getWeather` 和 `getForecast`
- [ ] 调用工具后返回实时天气数据
- [ ] 日志中看到 tool 调用记录

### 预期的 tools/list 响应:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "getWeather",
        "description": "Get current weather and forecast for a specific location by city name or coordinates",
        "inputSchema": {
          "type": "object",
          "properties": {
            "location": {"type": "string"}
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
            "location": {"type": "string"}
          },
          "required": ["location"]
        }
      }
    ]
  }
}
```

### 预期的 tools/call 响应:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Current Weather for shenzhen:\n\nTemperature: 15.0°C\n..."
      }
    ]
  }
}
```

## 🐛 故障排查

### 问题 0: JAR 文件损坏 "没有清单属性" 错误

**症状**:
- Cursor 日志显示: `D:/projects/originmcp/target/originmcp-0.0.1-SNAPSHOT.jar没有清单属性`
- MCP Server 无法启动

**原因**: Maven 构建过程被中断，JAR 文件不完整

**解决方案**:

**方法 1: 使用提供的重建脚本（推荐）**
```bash
# 1. 完全关闭 Cursor
# 2. 运行重建脚本
rebuild-mcp.bat

# 3. 等待构建完成
# 4. 重新打开 Cursor
```

**方法 2: 手动重建**
```bash
# 1. 完全关闭 Cursor（确保 JAR 文件不被锁定）

# 2. 删除 target 目录
rmdir /S /Q target

# 3. 重新构建
mvn clean package -DskipTests

# 4. 验证 JAR 文件
java -jar target\originmcp-0.0.1-SNAPSHOT.jar --version

# 5. 重新打开 Cursor
```

**方法 3: 如果文件被锁定**
```bash
# 查找锁定进程
wmic process where "commandline like '%originmcp%'" get processid,commandline

# 手动终止进程
taskkill /F /PID <进程ID>

# 然后重新构建
mvn clean package -DskipTests
```

### 问题 1: Cursor/VSCode 没有识别 MCP Server

**解决方案**:
1. 确认 JAR 路径是绝对路径
2. 检查 Java 是否在 PATH 中: `java -version`
3. 手动运行 JAR 测试: `java -jar target\originmcp-0.0.1-SNAPSHOT.jar`
4. 查看 AI 助手的日志/设置

### 问题 2: MCP Server 启动失败

**检查**:
```bash
# 手动启动查看错误
java -jar target\originmcp-0.0.1-SNAPSHOT.jar

# 查看完整日志
java -jar target\originmcp-0.0.1-SNAPSHOT.jar --debug
```

### 问题 3: 工具没有暴露

**验证**:
```bash
# 使用测试脚本
node test-mcp-server.js

# 查看输出中是否包含:
# - getWeather
# - getForecast
```

### 问题 4: 工具调用失败

**调试**:
1. 检查日志中的异常
2. 验证参数格式
3. 测试 REST API 是否正常: `curl http://localhost:8080/api/weather/current?location=shenzhen`

## 📝 测试用例

### 测试 1: 查询单个城市天气

**在 AI 助手中输入**:
```
深圳现在天气怎么样?
```

**预期**: 返回深圳当前温度、湿度、风速、天气状况

### 测试 2: 查询7天预报

**输入**:
```
帮我查一下北京未来一周的天气预报
```

**预期**: 返回北京未来7天的天气信息

### 测试 3: 使用坐标查询

**输入**:
```
查询坐标 22.5431,114.0579 的天气
```

**预期**: 返回对应位置的天气

### 测试 4: 多城市对比

**输入**:
```
比较一下北京和上海的天气
```

**预期**: AI 调用两次 getWeather 工具,返回对比结果

## 🎓 团队使用建议

### 1. 统一配置管理

建议团队统一 MCP Server 配置位置:

```
<项目根目录>/.mcp/config.json
```

### 2. 环境变量

使用环境变量管理路径:

```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": [
        "-jar",
        "${MCP_WEATHER_JAR}"
      ]
    }
  }
}
```

### 3. 日志记录

在 `application.properties` 中启用日志:

```properties
logging.level.com.bitsoft.originmcp=DEBUG
logging.file.name=logs/mcp-server.log
```

### 4. 健康检查

添加健康检查端点:

```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

## 🚀 下一步

1. ✅ 验证 MCP Server 工作正常
2. ✅ 团队成员配置各自的 AI 助手
3. ✅ 开发更多通用工具
4. ✅ 建立 MCP Server 开发规范

---

**快速验证命令**:
```bash
# 1. 打包
mvn clean package

# 2. 测试
node test-mcp-server.js

# 3. 在 Cursor 中验证
# 询问: "深圳现在天气怎么样?"
```

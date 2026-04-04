# Weather MCP Server - 使用文档

## 项目简介

本项目是一个 Spring AI **Model Context Protocol (MCP) Server**,提供实时天气查询功能。可以通过 VSCode 配置或 REST API 调用。

## 特性

✅ **支持 MCP 协议** - 可被 AI 助手(如 Claude)直接调用
✅ **支持 REST API** - 可通过 HTTP 请求调用
✅ **完全免费** - 使用 Open-Meteo 免费天气 API,无需注册
✅ **支持中英文** - 支持中文城市名和坐标查询
✅ **实时数据** - 获取最新天气数据和7天预报

## 天气数据来源

使用 **Open-Meteo** 免费天气 API:
- 官网: https://open-meteo.com/
- 完全免费,无需 API Key
- 数据覆盖全球
- 每天可免费请求 10,000 次

## MCP 工具说明

本服务提供两个 MCP 工具:

### 1. `getWeather` - 获取当前天气
**描述**: 获取指定位置的当前天气信息

**参数**:
- `location` (string) - 城市名称或坐标

**支持的城市**:
- 中文: 北京、上海、深圳、广州
- 英文: beijing, shanghai, shenzhen, guangzhou, new york, london, tokyo, paris, los angeles
- 我现在修改一下描述信息

**坐标格式**: `纬度,经度` (例如: `39.9042,116.4074`)

**示例调用**:
```
getWeather("北京")
getWeather("shanghai")
getWeather("39.9042,116.4074")
```

### 2. `getForecast` - 获取7天预报
**描述**: 获取指定位置的详细7天天气预报

**参数**:
- `location` (string) - 城市名称或坐标

**示例调用**:
```
getForecast("上海")
getForecast("new york")
getForecast("31.2304,121.4737")
```

## 如何使用 MCP Server

### 方式 1: 直接使用 REST API (推荐,最简单)

启动服务后,直接通过 HTTP 请求调用:

```bash
# 启动服务
./mvnw spring-boot:run   # Linux/Mac
mvnw.cmd spring-boot:run  # Windows

# 调用 API
curl "http://localhost:8080/api/weather/current?location=shenzhen"
curl "http://localhost:8080/api/weather/forecast?location=beijing"
```

### 方式 2: 在 Claude Desktop 中配置 MCP

如果你使用 **Claude Desktop 应用**,可以配置 MCP Server:

**步骤 1: 启动 MCP Server**
```bash
./mvnw spring-boot:run
```

**步骤 2: 找到 Claude Desktop 配置文件**

配置文件位置:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

如果文件不存在,手动创建它。

**步骤 3: 编辑配置文件**

添加以下内容:
```json
{
  "mcpServers": {
    "weather": {
      "command": "java",
      "args": [
        "-jar",
        "path/to/your/originmcp-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

或者,如果你的 MCP Server 已经运行,配置为 HTTP 模式:
```json
{
  "mcpServers": {
    "weather": {
      "url": "http://localhost:8080/mcp",
      "transport": "sse"
    }
  }
}
```

**步骤 4: 重启 Claude Desktop**

重启后,在 Claude 中可以直接问:
- "北京现在天气怎么样?"
- "帮我查一下上海未来一周的天气预报"
- "New York 的天气如何?"

### 方式 3: 在代码中直接调用 (编程方式)

如果你在开发其他应用,可以直接调用 WeatherService:

```java
@Autowired
private WeatherService weatherService;

public void checkWeather() {
    String weather = weatherService.getWeather("beijing");
    System.out.println(weather);

    String forecast = weatherService.getForecast("shanghai");
    System.out.println(forecast);
}
```

### 注意事项

1. **MCP 协议支持**: 本项目的 `@Tool` 注解是为 Spring AI MCP Server 准备的,但具体的 MCP 协议实现可能需要额外配置。

2. **最简单的使用方式**: 直接使用 REST API 是最可靠的方式,不需要复杂配置。

3. **VSCode 集成**: 如果需要在 VSCode 中使用,推荐直接调用 REST API,而不是依赖 MCP 协议。

## REST API 使用

### 1. 获取当前天气

**端点**: `GET /api/weather/current`

**参数**:
- `location` (string, 必需) - 城市名或坐标

**示例**:
```bash
# 使用城市名
curl "http://localhost:8080/api/weather/current?location=beijing"
curl "http://localhost:8080/api/weather/current?location=北京"

# 使用坐标
curl "http://localhost:8080/api/weather/current?location=39.9042,116.4074"
```

**响应示例**:
```
Current Weather for beijing:

Temperature: 15.2°C
Humidity: 45%
Wind Speed: 12.5 km/h
Conditions: Partly cloudy
```

### 2. 获取7天预报

**端点**: `GET /api/weather/forecast`

**参数**:
- `location` (string, 必需) - 城市名或坐标

**示例**:
```bash
curl "http://localhost:8080/api/weather/forecast?location=shanghai"
```

**响应示例**:
```
7-Day Forecast for shanghai:

2026-01-03:
  High: 18.5°C, Low: 12.3°C
  Partly cloudy

2026-01-04:
  High: 20.1°C, Low: 13.7°C
  Clear sky

...
```

## 项目结构

```
src/main/java/com/bitsoft/originmcp/
├── controller/
│   └── WeatherController.java      # REST API 控制器
├── service/
│   └── WeatherService.java         # 天气服务 (带 @Tool 注解)
└── OriginmcpApplication.java       # Spring Boot 主类
```

## 技术栈

- **Spring Boot 3.5.9** - 应用框架
- **Spring AI 1.1.2** - MCP Server 支持
- **Java 21** - 编程语言
- **Open-Meteo API** - 天气数据源
- **RestClient** - HTTP 客户端

## 支持的城市列表

| 中文 | 英文 | 坐标 |
|------|------|------|
| 北京 | beijing | 39.9042, 116.4074 |
| 上海 | shanghai | 31.2304, 121.4737 |
| 深圳 | shenzhen | 22.5431, 114.0579 |
| 广州 | guangzhou | 23.1291, 113.2644 |
| - | new york, nyc | 40.7128, -74.0060 |
| - | london | 51.5074, -0.1278 |
| - | tokyo | 35.6762, 139.6503 |
| - | paris | 48.8566, 2.3522 |
| - | los angeles, la | 34.0522, -118.2437 |

**添加更多城市**: 修改 `WeatherService.getCityCoordinates()` 方法

## 天气代码说明

| 代码 | 天气状况 |
|------|----------|
| 0 | Clear sky (晴天) |
| 1-3 | Partly cloudy (部分多云) |
| 45, 48 | Foggy (雾) |
| 51-55 | Drizzle (毛毛雨) |
| 61-65 | Rain (雨) |
| 71-75 | Snow (雪) |
| 80-82 | Rain showers (阵雨) |
| 85-86 | Snow showers (阵雪) |
| 95 | Thunderstorm (雷暴) |
| 96-99 | Thunderstorm with hail (冰雹) |

## 运行和测试

### 启动应用
```bash
./mvnw spring-boot:run
```

### 运行测试
```bash
./mvnw test
```

### 测试 MCP 工具
```bash
# 测试当前天气
curl "http://localhost:8080/api/weather/current?location=北京"

# 测试7天预报
curl "http://localhost:8080/api/weather/forecast?location=shanghai"
```

## 常见问题

### Q: 为什么使用 Open-Meteo 而不是其他天气 API?
A: Open-Meteo 完全免费,无需注册和 API key,非常适合学习和演示项目。

### Q: 如何添加更多城市?
A: 在 `WeatherService.java` 的 `getCityCoordinates()` 方法中添加城市和坐标映射。

### Q: 支持其他语言吗?
A: 当前支持中英文城市名,可以通过修改 `getCityCoordinates()` 方法支持更多语言。

### Q: MCP Server 和 REST API 可以同时使用吗?
A: 是的,本项目同时支持 MCP 协议和标准 REST API。

### Q: 如何在 Claude Desktop 中使用?
A: 在 Claude Desktop 的配置文件中添加本 MCP Server 的地址即可。

## 开发者信息

- **API 文档**: https://open-meteo.com/en/docs
- **MCP 协议**: https://docs.spring.io/spring-ai/reference/api/mcp/
- **Spring AI**: https://docs.spring.io/spring-ai/reference/

## License

本项目仅用于学习和演示目的。

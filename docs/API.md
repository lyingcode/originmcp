# Weather API - REST 接口文档

## 项目改造说明

本项目已从 Spring AI MCP Server 改造为标准的 REST API 应用,移除了所有 AI 相关的注解和依赖。

### 改造内容

1. ✅ 移除了 `@Tool` 和 `@ToolParam` 注解
2. ✅ 移除了 `spring-ai-starter-mcp-server` 依赖
3. ✅ 移除了 `spring-ai-bom` 依赖管理
4. ✅ 创建了标准的 REST Controller
5. ✅ 添加了 Controller 层的单元测试

## API 端点

### 1. 获取天气预报

**端点**: `GET /api/weather/forecast`

**参数**:
- `latitude` (double, 必需) - 纬度
- `longitude` (double, 必需) - 经度

**示例请求**:
```bash
curl "http://localhost:8080/api/weather/forecast?latitude=39.7456&longitude=-97.0892"
```

**示例响应**:
```
Weather Forecast:

Tonight:
  Temperature: 45°F
  Conditions: Clear
  Details: Clear skies with light winds.

Tomorrow:
  Temperature: 68°F
  Conditions: Sunny
  Details: Sunny day with gentle breeze.
```

### 2. 获取天气警报

**端点**: `GET /api/weather/alerts/{state}`

**路径参数**:
- `state` (string, 必需) - 美国州代码 (例如: CA, NY, TX)

**示例请求**:
```bash
curl "http://localhost:8080/api/weather/alerts/CA"
```

**示例响应** (有警报时):
```
Active Weather Alerts:

Event: Heat Advisory
Severity: Moderate
Area: Los Angeles County
Headline: Heat Advisory issued for Los Angeles County

Event: Wind Warning
Severity: Severe
Area: San Diego County
Headline: Wind Warning issued for San Diego County
```

**示例响应** (无警报时):
```
No active weather alerts for this state.
```

## 运行项目

### 1. 启动应用
```bash
./mvnw spring-boot:run
```

或在 Windows 上:
```bash
mvnw.cmd spring-boot:run
```

### 2. 运行测试
```bash
./mvnw test
```

## 项目结构

```
src/main/java/com/bitsoft/originmcp/
├── controller/
│   └── WeatherController.java      # REST API 控制器
├── service/
│   └── WeatherService.java         # 天气服务业务逻辑
└── OriginmcpApplication.java       # Spring Boot 主类

src/test/java/com/bitsoft/originmcp/
├── controller/
│   └── WeatherControllerTest.java  # Controller 测试
└── service/
    └── WeatherServiceTest.java     # Service 测试
```

## 技术栈

- **Spring Boot 3.5.9**
- **Java 21**
- **JUnit 5** - 单元测试框架
- **Mockito** - Mock 框架
- **Spring Web** - REST API 支持
- **RestClient** - HTTP 客户端

## 数据源

本项目使用美国国家气象局 (National Weather Service) 的公开 API:
- 基础 URL: `https://api.weather.gov`
- 文档: https://www.weather.gov/documentation/services-web-api

## 测试覆盖

### WeatherServiceTest (11个测试用例)
- ✅ 成功获取天气预报
- ✅ 无法获取预报 URL
- ✅ API 异常处理
- ✅ 边界情况坐标测试
- ✅ 多时段格式化
- ✅ 活动警报查询
- ✅ 无警报状态
- ✅ 小写州代码转换
- ✅ 警报 API 异常
- ✅ 缺少 features 字段
- ✅ 部分警报数据

### WeatherControllerTest (5个测试用例)
- ✅ 成功获取预报
- ✅ 缺少请求参数
- ✅ 成功获取警报
- ✅ 无警报状态
- ✅ 小写州代码处理

## 注意事项

1. **请求限制**: 请遵守 NWS API 的使用限制
2. **User-Agent**: 建议在 WeatherService 中设置有效的联系邮箱
3. **仅限美国**: 此 API 仅支持美国境内的天气数据

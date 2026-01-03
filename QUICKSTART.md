# 天气查询服务 - 快速使用指南

## 🚀 快速开始 (1分钟)

### 1. 服务已启动
你的服务已经在运行: `http://localhost:8080` ✅

### 2. 直接查询天气

#### 方法 1: 使用 curl (命令行)
```bash
# 查询当前天气
curl "http://localhost:8080/api/weather/current?location=shenzhen"

# 查询7天预报
curl "http://localhost:8080/api/weather/forecast?location=beijing"
```

#### 方法 2: 使用浏览器
直接在浏览器地址栏输入:
```
http://localhost:8080/api/weather/current?location=shenzhen
http://localhost:8080/api/weather/forecast?location=shanghai
```

## 📍 支持的位置格式

### 1. 中文城市名
```bash
curl "http://localhost:8080/api/weather/current?location=北京"
curl "http://localhost:8080/api/weather/current?location=上海"
curl "http://localhost:8080/api/weather/current?location=深圳"
curl "http://localhost:8080/api/weather/current?location=广州"
```

**注意**: 中文 URL 在 curl 中可能需要编码,建议使用英文或坐标。

### 2. 英文城市名 (推荐)
```bash
curl "http://localhost:8080/api/weather/current?location=beijing"
curl "http://localhost:8080/api/weather/current?location=shanghai"
curl "http://localhost:8080/api/weather/current?location=shenzhen"
curl "http://localhost:8080/api/weather/current?location=guangzhou"
curl "http://localhost:8080/api/weather/current?location=newyork"
curl "http://localhost:8080/api/weather/current?location=london"
```

### 3. 坐标格式 (支持全球任意位置)
```bash
# 深圳
curl "http://localhost:8080/api/weather/current?location=22.5431,114.0579"

# 北京
curl "http://localhost:8080/api/weather/current?location=39.9042,116.4074"

# 纽约
curl "http://localhost:8080/api/weather/current?location=40.7128,-74.0060"
```

## 📊 API 端点

### 1. 获取当前天气
- **端点**: `/api/weather/current`
- **参数**: `location` (城市名或坐标)
- **返回**: 温度、湿度、风速、天气状况

**示例**:
```bash
curl "http://localhost:8080/api/weather/current?location=shenzhen"
```

**响应**:
```
Current Weather for shenzhen:

Temperature: 15.0°C
Humidity: 52%
Wind Speed: 7.6 km/h
Conditions: Clear sky
```

### 2. 获取7天预报
- **端点**: `/api/weather/forecast`
- **参数**: `location` (城市名或坐标)
- **返回**: 未来7天的最高/最低温度和天气

**示例**:
```bash
curl "http://localhost:8080/api/weather/forecast?location=beijing"
```

## 🌍 内置城市列表

| 中文 | 英文 | 别名 |
|------|------|------|
| 北京 | beijing | - |
| 上海 | shanghai | - |
| 深圳 | shenzhen | - |
| 广州 | guangzhou | - |
| - | new york | nyc |
| - | london | - |
| - | tokyo | - |
| - | paris | - |
| - | los angeles | la |

## 💡 使用技巧

### 1. Windows PowerShell 中使用
```powershell
Invoke-RestMethod "http://localhost:8080/api/weather/current?location=shenzhen"
```

### 2. 在 Python 中调用
```python
import requests

# 查询当前天气
response = requests.get("http://localhost:8080/api/weather/current",
                       params={"location": "shenzhen"})
print(response.text)

# 查询7天预报
response = requests.get("http://localhost:8080/api/weather/forecast",
                       params={"location": "beijing"})
print(response.text)
```

### 3. 在 JavaScript 中调用
```javascript
// 使用 fetch
fetch('http://localhost:8080/api/weather/current?location=shenzhen')
  .then(response => response.text())
  .then(data => console.log(data));

// 使用 axios
axios.get('http://localhost:8080/api/weather/current', {
  params: { location: 'beijing' }
}).then(response => console.log(response.data));
```

## ❌ 常见问题

### Q: 中文城市名无法查询?
**A**: 在 URL 中使用中文可能需要编码,建议:
1. 使用英文城市名: `location=beijing`
2. 使用坐标: `location=39.9042,116.4074`

### Q: 提示城市不存在?
**A**:
1. 检查拼写是否正确
2. 使用坐标格式: `location=纬度,经度`
3. 查看内置城市列表,确认是否支持

### Q: 如何查询其他城市?
**A**: 使用坐标格式,可以查询全球任意位置:
```bash
# 先在地图上找到城市坐标,然后:
curl "http://localhost:8080/api/weather/current?location=纬度,经度"
```

### Q: 如何停止服务?
**A**: 在运行 `mvnw spring-boot:run` 的终端按 `Ctrl+C`

## 🔧 开发者信息

- **端口**: 8080
- **基础 URL**: http://localhost:8080
- **数据源**: Open-Meteo API (免费,无需注册)
- **更新频率**: 实时
- **支持范围**: 全球

## 📝 下一步

1. **添加更多城市**: 编辑 `WeatherService.java` 中的 `getCityCoordinates()` 方法
2. **集成到你的应用**: 直接调用 REST API
3. **查看完整文档**: 参考 `MCP-README.md`

---

**快速测试命令**:
```bash
# 测试服务是否运行
curl "http://localhost:8080/api/weather/current?location=shenzhen"
```

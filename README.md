# SunnyWeather

一款简洁优雅的 Android 天气应用，基于 Kotlin 开发，采用 MVVM 架构，支持自动定位、城市搜索和实时天气查询。

## 功能特性

### 核心功能

- **实时天气展示**
  - 当前温度、天气状况、空气质量指数
  - 动态天气背景（根据天气状况自动切换）

- **天气预报**
  - 未来多日天气预报
  - 每日最高/最低温度
  - 天气图标可视化展示

- **生活指数**
  - 穿衣建议
  - 感冒风险指数
  - 紫外线指数
  - 洗车指数

- **智能定位**
  - IP定位（优先，速度快、零功耗）
  - GPS定位（备用方案，精度高）
  - 高德地图逆地理编码
  - 自动保存上次查看的城市

- **城市搜索**
  - 支持全球城市搜索
  - 搜索结果列表展示
  - 快速切换查看不同城市天气

### UI/UX特性

- **Material Design设计风格**
- **Edge-to-Edge全屏显示**
- **下拉刷新**：快速更新天气数据
- **侧滑抽屉菜单**：便捷的城市切换入口
- **动态背景**：根据天气状况展示不同背景图（晴天、多云、雨天、雪天等）
- **响应式布局**：适配不同屏幕尺寸

## 技术架构

### 整体架构

采用 **MVVM (Model-View-ViewModel)** 架构模式：

```
├── UI Layer (Activity/Fragment)
│   ├── MainActivity
│   ├── PlaceFragment
│   └── WeatherActivity
├── ViewModel Layer
│   ├── PlaceViewModel
│   └── WeatherViewModel
├── Repository Layer
│   └── Repository (数据仓库)
├── Data Layer
│   ├── Model (数据模型)
│   ├── Network (网络请求)
│   └── DAO (数据持久化)
```

### 技术栈

| 类别 | 技术方案 |
|------|---------|
| 开发语言 | Kotlin |
| 架构模式 | MVVM |
| 异步处理 | Kotlin Coroutines (协程) |
| 网络请求 | Retrofit 2.11.0 |
| JSON解析 | Gson |
| 数据绑定 | ViewBinding |
| 数据观察 | LiveData + ViewModel |
| 列表展示 | RecyclerView |
| 下拉刷新 | SwipeRefreshLayout |
| UI组件 | Material Design Components |
| 数据持久化 | SharedPreferences + Gson |

### 数据源

- **天气数据**: [彩云天气API](https://www.caiyunapp.com/hp/)
  - 提供实时天气、天气预报、生活指数等数据

- **地理编码**: [高德地图API](https://lbs.amap.com/)
  - IP定位服务
  - 逆地理编码（经纬度转城市名）
  - 地点搜索服务

## 项目结构

```
app/src/main/java/com/sunnyweather/android/
├── SunnyWeatherApplication.kt          # Application类
├── MainActivity.kt                     # 主Activity（搜索入口）
├── logic/                              # 业务逻辑层
│   ├── dao/
│   │   └── PlaceDao.kt                 # 地点数据持久化
│   ├── model/
│   │   ├── PlaceResponse.kt            # 地点搜索响应模型
│   │   ├── RealtimeResponse.kt         # 实时天气响应模型
│   │   ├── DailyResponse.kt            # 天气预报响应模型
│   │   ├── IpLocationResponse.kt       # IP定位响应模型
│   │   ├── RegeoResponse.kt            # 逆地理编码响应模型
│   │   ├── Weather.kt                  # 天气数据封装
│   │   └── Sky.kt                      # 天气状况映射
│   ├── network/
│   │   ├── ServiceCreator.kt           # Retrofit服务创建
│   │   ├── SunnyWeatherNetWork.kt      # 网络请求统一入口
│   │   ├── PlaceService.kt             # 地点搜索接口
│   │   ├── WeatherService.kt           # 天气查询接口
│   │   ├── IpService.kt                # IP定位接口
│   │   └── AmapService.kt              # 高德地图接口
│   └── Repository.kt                   # 数据仓库（统一数据来源）
└── ui/                                 # UI层
    ├── place/
    │   ├── PlaceFragment.kt            # 地点搜索Fragment
    │   ├── PlaceViewModel.kt           # 地点搜索ViewModel
    │   └── PlaceAdapter.kt             # 地点列表适配器
    └── weather/
        ├── WeatherActivity.kt          # 天气展示Activity
        └── WeatherViewModel.kt         # 天气展示ViewModel
```

## 核心功能实现

### 1. 智能定位

采用多级降级策略确保定位成功率：

```
启动应用
    ↓
尝试IP定位 (高德IP服务)
    ↓ (失败)
尝试GPS缓存定位 (30分钟内有效)
    ↓ (失败)
尝试GPS实时定位 (10秒超时)
    ↓ (失败)
降级到已保存城市 / 手动搜索
```

### 2. 网络请求优化

- **429限流重试机制**：遇到API限流时自动重试最多3次
- **协程异步处理**：所有网络请求在IO线程执行
- **统一错误处理**：使用 `Result` 封装请求结果

### 3. 数据持久化

使用 SharedPreferences + Gson 保存用户选择的城市：

```kotlin
// 保存
PlaceDao.savePlace(place)

// 读取
val savedPlace = PlaceDao.getSavedPlace()

// 检查是否存在
val isSaved = PlaceDao.isPlaceSaved()
```

### 4. MVVM数据流

```
View (Activity/Fragment)
    ↓ (用户操作)
ViewModel
    ↓ (触发请求)
Repository
    ↓ (统一调度)
Network/DAO
    ↓ (返回数据)
LiveData
    ↓ (数据观察)
View (更新UI)
```

## 安装与运行

### 环境要求

- Android Studio Arctic Fox 或更高版本
- Android SDK 24+ (Android 7.0)
- JDK 11+

### API密钥配置

在 `SunnyWeatherApplication.kt` 中配置API密钥：

```kotlin
companion object {
    // 彩云天气API Token
    const val TOKEN = "your_caiyun_token"

    // 高德地图API Key
    const val AMAP_KEY = "your_amap_key"
}
```

**获取API密钥：**
- 彩云天气：https://www.caiyunapp.com/hp/
- 高德地图：https://lbs.amap.com/

### 编译运行

1. 克隆项目
```bash
git clone https://github.com/yourusername/SunnyWeather.git
```

2. 用 Android Studio 打开项目

3. 配置API密钥

4. 点击 Run 按钮或使用快捷键运行

## 权限说明

应用需要以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

- `INTERNET`：网络请求必需
- `ACCESS_COARSE_LOCATION`：粗略定位（网络定位）
- `ACCESS_FINE_LOCATION`：精确定位（GPS定位）

定位权限为可选权限，拒绝授权后仍可通过手动搜索使用应用。

## 界面预览

应用包含以下主要界面：

1. **启动页/搜索页**
   - 自动定位按钮
   - 城市搜索框
   - 搜索结果列表

2. **天气详情页**
   - 实时天气卡片（动态背景）
   - 未来天气预报列表
   - 生活指数卡片
   - 侧滑抽屉（切换城市）

## 扩展性设计

项目采用模块化设计，易于扩展：

- **添加新数据源**：在 `Repository` 中添加新的数据获取方法
- **扩展天气模型**：在 `model` 包中添加新的数据类
- **新增UI功能**：遵循MVVM模式，添加新的ViewModel和布局
- **切换数据源**：修改 `Repository` 中的网络请求实现

## 学习价值

本项目适合作为以下技术的学习案例：

- Kotlin语言基础与进阶特性
- MVVM架构模式实践
- Retrofit网络请求封装
- Kotlin协程异步编程
- LiveData数据响应式编程
- ViewBinding视图绑定
- Material Design UI设计
- 权限处理最佳实践
- 定位功能集成

## 开源协议

本项目基于 Apache 2.0 协议开源，详见 [LICENSE](LICENSE) 文件。

## 致谢

- 天气数据提供：[彩云天气](https://www.caiyunapp.com/)
- 地理编码服务：[高德地图](https://lbs.amap.com/)
- 开发灵感来源：《第一行代码 Android》

---

**开发者**: SunnyWeather Team
**最后更新**: 2024

# 🐟图 - 智能协同云图库

<div align="center">

![🐟图 Logo](https://img.shields.io/badge/🐟图-智能协同云图库-blue?style=for-the-badge)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.13-brightgreen.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6.3-blue.svg)](https://www.typescriptlang.org/)

**基于 Vue 3 + Spring Boot + COS + WebSocket 的企业级智能协同云图库平台**

[项目文档](https://github.com/Fishstarpro/yu-picture/wiki) | [问题反馈](https://github.com/Fishstarpro/yu-picture/issues)

</div>

## 📖 项目简介

🐟图是一个基于 Vue 3 + Spring Boot + COS + WebSocket 的企业级智能协同云图库平台。项目应用场景广泛，可作为表情包网站、设计素材网站、壁纸网站、个人云盘、企业活动相册等。用户可以在平台公开上传和检索图片素材；管理员可以上传、审核和管理分析图片；个人用户可将图片上传至私有空间进行批量管理、检索、编辑和分析；企业可开通团队空间并邀请成员，共享图片并实时协作。

### ✨ 核心特色

- 🚀 **高性能架构** - 基于 Spring Boot 2.7 + Vue 3 + TypeScript 构建
- 🎨 **现代化UI** - 采用 Ant Design Vue 4.x，界面美观易用
- 🔍 **智能搜索** - 支持按名称、标签、分类等多维度搜索
- 📁 **空间管理** - 支持个人空间和团队空间，灵活的权限控制
- 🛡️ **安全可靠** - 集成 Sa-Token 权限认证，支持图片审核机制
- 📊 **数据分析** - 提供图片统计分析和可视化图表
- 🌐 **实时协作** - 基于 WebSocket 的实时协同功能
- ☁️ **云端存储** - 集成腾讯云 COS 对象存储服务
- 📱 **响应式设计** - 完美适配桌面端和移动端

## 🏗️ 项目架构

```
yu-picture/
├── yu-picture-backend/          # 后端服务
│   ├── src/main/java/
│   │   └── com/yxc/yupicturebackend/
│   │       ├── controller/      # 控制器层
│   │       ├── service/         # 业务逻辑层
│   │       ├── mapper/          # 数据访问层
│   │       ├── model/           # 数据模型
│   │       ├── config/          # 配置类
│   │       ├── utils/           # 工具类
│   │       └── ...
│   ├── sql/                     # 数据库脚本
│   └── pom.xml                  # Maven 配置
├── yu-picture-frontend/         # 前端应用
│   ├── src/
│   │   ├── pages/               # 页面组件
│   │   ├── components/          # 通用组件
│   │   ├── api/                 # API 接口
│   │   ├── stores/              # 状态管理
│   │   ├── router/              # 路由配置
│   │   └── ...
│   ├── package.json             # 依赖配置
│   └── vite.config.ts           # 构建配置
└── README.md                    # 项目说明
```

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 2.7.6 | 应用框架 |
| MyBatis Plus | 3.5.9 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存数据库 |
| Sa-Token | 1.39.0 | 权限认证框架 |
| Knife4j | 4.4.0 | API 文档工具 |
| Hutool | 5.8.37 | Java 工具库 |
| Caffeine | 3.1.8 | 本地缓存 |
| ShardingSphere | 5.2.0 | 分库分表 |
| Disruptor | 3.4.2 | 高性能队列 |
| Tencent COS | 5.6.227 | 对象存储 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.13 | 前端框架 |
| TypeScript | 5.6.3 | 编程语言 |
| Vite | 6.0.1 | 构建工具 |
| Ant Design Vue | 4.2.6 | UI 组件库 |
| Vue Router | 4.4.5 | 路由管理 |
| Pinia | 2.2.6 | 状态管理 |
| Axios | 1.7.9 | HTTP 客户端 |
| ECharts | 5.6.0 | 数据可视化 |
| Vue Cropper | 1.1.4 | 图片裁剪 |

## 🎯 功能特性

### 🔐 用户系统
- 用户注册、登录、注销
- 个人信息管理
- 权限角色控制（用户/管理员）
- 安全认证机制

### 📸 图片管理
- 图片上传（支持单张/批量）
- 图片预览和详情查看
- 图片编辑（裁剪、调色等）
- 图片分类和标签管理
- 图片搜索和筛选
- 图片审核机制

### 🏢 空间系统
- 个人空间管理
- 团队空间创建
- 空间成员管理
- 权限级别控制（查看者/编辑者/管理员）
- 空间容量管理

### 📊 数据分析
- 图片统计分析
- 用户行为分析
- 空间使用情况
- 可视化图表展示

### 🔧 管理功能
- 用户管理
- 图片审核
- 空间管理
- 系统配置

## 🚀 快速开始

### 环境要求

- **Java**: 17+
- **Node.js**: 16.0+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Maven**: 3.6+

### 后端部署

1. **克隆项目**
```bash
git clone https://github.com/Fishstarpro/yu-picture.git
cd yu-picture
```

2. **配置数据库**
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE yu_picture;

# 导入数据表
mysql -u root -p yu_picture < yu-picture-backend/sql/create_table.sql
```

3. **配置应用**
```bash
# 复制配置文件
cd yu-picture-backend/src/main/resources
cp application.yml.example application.yml

# 修改数据库配置
vim application.yml
```

4. **启动后端服务**
```bash
cd yu-picture-backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8111` 启动

### 前端部署

1. **安装依赖**
```bash
cd yu-picture-frontend
npm install
```

2. **配置环境**
```bash
# 复制环境配置
cp .env.example .env.local

# 修改 API 地址
vim .env.local
```

3. **启动开发服务**
```bash
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

4. **构建生产版本**
```bash
npm run build
```



## 📝 API 文档

启动后端服务后，访问以下地址查看 API 文档：

- **Swagger UI**: http://localhost:8111/doc.html
- **OpenAPI JSON**: http://localhost:8111/v2/api-docs

## 🔧 配置说明

### 后端配置

主要配置文件：`yu-picture-backend/src/main/resources/application.yml`

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/yu_picture
    username: your_username
    password: your_password
  
  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: your_redis_password

# 对象存储配置
cos:
  client:
    accessKey: your_access_key
    secretKey: your_secret_key
    region: your_region
    bucket: your_bucket
```

### 前端配置

主要配置文件：`yu-picture-frontend/.env.local`

```env
# API 基础地址
VITE_API_BASE_URL=http://localhost:8111

# 应用标题
VITE_APP_TITLE=🐟图

# 其他配置...
```

## 🤝 贡献指南

我们欢迎所有形式的贡献！请遵循以下步骤：

1. **Fork 项目**
2. **创建特性分支** (`git checkout -b feature/AmazingFeature`)
3. **提交更改** (`git commit -m 'Add some AmazingFeature'`)
4. **推送到分支** (`git push origin feature/AmazingFeature`)
5. **创建 Pull Request**

### 开发规范

- 遵循 [Java 编码规范](https://google.github.io/styleguide/javaguide.html)
- 遵循 [Vue.js 风格指南](https://vuejs.org/style-guide/)
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- 确保代码通过所有测试

## 📄 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。

## 🙏 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Ant Design Vue](https://antdv.com/)
- [MyBatis Plus](https://baomidou.com/)
- [Sa-Token](https://sa-token.cc/)

## 📞 联系我们

- **项目地址**: https://github.com/Fishstarpro/yu-picture
- **问题反馈**: https://github.com/Fishstarpro/yu-picture/issues
- **邮箱**: yushenxiansheng@163.com

---

<div align="center">

**如果这个项目对你有帮助，请给它一个 ⭐️**

Made with ❤️ by [Fishstarpro](https://github.com/Fishstarpro)

</div> 
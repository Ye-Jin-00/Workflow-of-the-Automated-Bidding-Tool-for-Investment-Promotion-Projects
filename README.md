# 招商智投 · 招商项目自动投标工具工作流

Workflow of the Automated Bidding Tool for Investment Promotion Projects

面向产业招商业务的投标工作流原型，将「招商线索 → 资格预审 → 招标要素抽取 → 资质匹配与报价 → 标书初稿 → 人工复核 → 提交归档」固化为可运行、可追溯、可复用的系统。

## 技术栈

- 后端：Spring Boot 2.6.13（Java 8+）
- 前端：Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router
- 存储：JSON 文件（演示版，零外部依赖）
- AI：本地模板兜底，可选 OpenAI 兼容大模型（DeepSeek 等）

## 目录结构

```text
招商项目自动投标工具工作流/
├── code/
│   ├── backend/     Spring Boot 后端
│   ├── frontend/    Vue 3 前端
│   └── tools/       辅助脚本
└── doc/             文档集（需求、架构、接口、操作手册等）
```

## 快速启动与使用

启动步骤、演示账号、AI 配置与测试详见 [code/README.md](招商项目自动投标工具工作流/code/README.md)。

## 文档

- [文档总览与阅读指南](招商项目自动投标工具工作流/doc/00-文档总览与阅读指南.docx)
- [业务链路与需求说明](招商项目自动投标工具工作流/doc/01-业务链路与需求说明.docx)
- [技术架构与安全合规说明](招商项目自动投标工具工作流/doc/02-技术架构与安全合规说明.docx)
- [系统运行与接口说明](招商项目自动投标工具工作流/doc/03-系统运行与接口说明.docx)
- [功能说明与操作手册](招商项目自动投标工具工作流/doc/04-功能说明与操作手册.docx)

package com.aiblog.config;

import com.aiblog.entity.*;
import com.aiblog.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminRepo;
    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.default-username}")
    private String adminUsername;
    @Value("${app.admin.default-password}")
    private String adminPassword;

    public DataSeeder(AdminUserRepository adminRepo, PostRepository postRepo, SkillRepository skillRepo,
                      McpRepository mcpRepo, ApiStationRepository apiRepo, PasswordEncoder encoder) {
        this.adminRepo = adminRepo;
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedSkills();
        seedMcps();
        seedApiStations();
        seedPosts();
    }

    private void seedAdmin() {
        if (!adminRepo.existsByUsername(adminUsername)) {
            AdminUser u = new AdminUser();
            u.setUsername(adminUsername);
            u.setPasswordHash(encoder.encode(adminPassword));
            u.setRole("ADMIN");
            adminRepo.save(u);
            System.out.println(">>> 已创建默认管理员: " + adminUsername);
        }
    }

    private void seedSkills() {
        if (skillRepo.count() > 0) return;
        skillRepo.save(buildSkill("Claude Artifacts",
                "在对话中直接生成可运行的代码、网页和交互式组件，所见即所得。",
                "https://www.anthropic.com", "代码,生产力,前端", "生产力", 5));
        skillRepo.save(buildSkill("提示词工程 (Prompt Engineering)",
                "通过结构化提示词、Few-shot 示例、思维链等技巧显著提升模型输出质量。",
                "https://www.promptingguide.ai", "提示词,入门,通用", "技巧", 5));
        skillRepo.save(buildSkill("RAG 检索增强生成",
                "将外部知识库与大模型结合，让回答有据可依、减少幻觉。",
                "https://python.langchain.com", "RAG,知识库,进阶", "进阶", 4));
        skillRepo.save(buildSkill("Function Calling 工具调用",
                "让模型调用外部函数/API，实现查天气、查数据库等真实操作。",
                "https://platform.openai.com/docs/guides/function-calling", "工具,Agent,开发", "开发", 4));
    }

    private void seedMcps() {
        if (mcpRepo.count() > 0) return;
        mcpRepo.save(buildMcp("filesystem",
                "官方文件系统 MCP，让 AI 安全地读写本地文件。",
                "https://github.com/modelcontextprotocol/servers",
                "npx -y @modelcontextprotocol/server-filesystem /path", "官方,文件,基础", "官方", 5));
        mcpRepo.save(buildMcp("github",
                "GitHub MCP，让 AI 管理仓库、Issue、PR。",
                "https://github.com/modelcontextprotocol/servers",
                "npx -y @modelcontextprotocol/server-github", "官方,GitHub,协作", "官方", 5));
        mcpRepo.save(buildMcp("chrome-devtools",
                "浏览器自动化 MCP，让 AI 操作 Chrome、截图、调试性能。",
                "https://github.com/ChromeDevTools/chrome-devtools-mcp",
                "npx chrome-devtools-mcp@latest", "浏览器,自动化,调试", "自动化", 4));
        mcpRepo.save(buildMcp("postgres",
                "PostgreSQL MCP，让 AI 查询和分析数据库。",
                "https://github.com/modelcontextprotocol/servers",
                "npx -y @modelcontextprotocol/server-postgres", "数据库,查询", "数据库", 4));
    }

    private void seedApiStations() {
        if (apiRepo.count() > 0) return;
        apiRepo.save(buildApi("OpenAI 官方",
                "https://api.openai.com",
                "OpenAI 官方 API，作为可用性对照基准。",
                "gpt-4o,gpt-4o-mini,o1", "官方,对照"));
        apiRepo.save(buildApi("公益中转示例站 A",
                "https://api.example-free-a.com",
                "示例公益中转站，请在后台替换为真实地址。", "gpt-4o,claude-3.5", "公益,中转,示例"));
        apiRepo.save(buildApi("公益中转示例站 B",
                "https://api.example-free-b.com",
                "示例公益中转站，请在后台替换为真实地址。", "gemini-1.5,deepseek", "公益,中转,示例"));
    }

    private void seedPosts() {
        if (postRepo.count() > 0) return;
        Post p1 = new Post();
        p1.setTitle("新手入门：如何用好 AI 助手");
        p1.setSlug("ai-assistant-getting-started");
        p1.setSummary("从零开始了解如何与 AI 助手高效协作，包含提示词技巧与常见误区。");
        p1.setCategory("入门");
        p1.setTags("入门,提示词,教程");
        p1.setPublished(true);
        p1.setBodyMarkdown("""
                # 新手入门：如何用好 AI 助手

                AI 助手已经成为日常工作的得力帮手。本文带你快速上手。

                ## 1. 把需求说清楚

                与其问"帮我写点东西"，不如明确：
                - **目标**：要写什么？
                - **受众**：给谁看？
                - **格式**：多长、什么风格？

                ## 2. 善用示例（Few-shot）

                给出一两个范例，模型会更好地理解你的预期。

                ## 3. 分步骤推理

                复杂任务可以让模型 **一步步思考**，质量更高。

                > 提示：把大任务拆成小任务，逐个击破。

                ```python
                # 示例：调用 API
                import requests
                resp = requests.post(url, json=payload)
                print(resp.json())
                ```

                祝你玩得开心！
                """);
        postRepo.save(p1);

        Post p2 = new Post();
        p2.setTitle("MCP 是什么？一文看懂模型上下文协议");
        p2.setSlug("what-is-mcp");
        p2.setSummary("MCP（Model Context Protocol）让 AI 安全地连接外部工具与数据，本文讲清原理与配置。");
        p2.setCategory("进阶");
        p2.setTags("MCP,协议,工具");
        p2.setPublished(true);
        p2.setBodyMarkdown("""
                # MCP 是什么？

                **MCP（Model Context Protocol）** 是一个开放协议，让 AI 应用以标准方式连接外部数据源和工具。

                ## 核心概念

                | 概念 | 说明 |
                |------|------|
                | Server | 提供能力（文件、数据库、API）的一方 |
                | Client | AI 应用，消费这些能力 |
                | Tool | 可被模型调用的具体操作 |

                ## 快速配置一个文件系统 MCP

                ```bash
                npx -y @modelcontextprotocol/server-filesystem /your/path
                ```

                配置完成后，AI 就能安全地读写指定目录的文件了。
                """);
        postRepo.save(p2);
    }

    // ---- builders ----
    private Skill buildSkill(String name, String desc, String link, String tags, String cat, int level) {
        Skill s = new Skill();
        s.setName(name); s.setDescription(desc); s.setLink(link);
        s.setTags(tags); s.setCategory(cat); s.setRecommendLevel(level);
        return s;
    }

    private Mcp buildMcp(String name, String desc, String repo, String cmd, String tags, String cat, int level) {
        Mcp m = new Mcp();
        m.setName(name); m.setDescription(desc); m.setRepoUrl(repo); m.setInstallCmd(cmd);
        m.setTags(tags); m.setCategory(cat); m.setRecommendLevel(level);
        return m;
    }

    private ApiStation buildApi(String name, String url, String desc, String models, String tags) {
        ApiStation a = new ApiStation();
        a.setName(name); a.setBaseUrl(url); a.setDescription(desc);
        a.setSupportedModels(models); a.setTags(tags);
        return a;
    }
}

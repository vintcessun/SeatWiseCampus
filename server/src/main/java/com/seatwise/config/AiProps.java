package com.seatwise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 助手配置（OpenAI 兼容 Chat Completions 接口）。
 * 未配置 apiKey 时自动降级为规则引擎，保证离线可演示。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProps {
    /** OpenAI 兼容服务的 base-url，如 https://api.openai.com/v1 或 https://api.deepseek.com/v1 */
    private String baseUrl = "";
    /** API Key；为空则关闭 LLM，走规则引擎 */
    private String apiKey = "";
    /** 模型名，如 gpt-4o-mini / deepseek-chat / qwen-plus */
    private String model = "gpt-4o-mini";
    /** 单次超时（毫秒） */
    private int timeoutMs = 12000;

    public boolean llmEnabled() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }
}

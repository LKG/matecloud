# LLM 提供商

MateCloud 内置 6 个 LLM 提供商，通过环境变量切换。

## Anthropic Claude（默认）

```bash
export ANTHROPIC_API_KEY=sk-ant-xxx
```

## 智谱 GLM

```bash
export MATE_AI_PROVIDER=zhipuai
export ZHIPUAI_API_KEY=your-key
```

## DeepSeek

```bash
export MATE_AI_PROVIDER=deepseek
export DEEPSEEK_API_KEY=your-key
```

## OpenAI 兼容（Kimi / Moonshot 等）

```bash
export MATE_AI_PROVIDER=openai
export OPENAI_API_KEY=your-key
export OPENAI_BASE_URL=https://api.moonshot.cn
export OPENAI_MODEL=moonshot-v1-32k
```

## Minimax

```bash
export MATE_AI_PROVIDER=minimax
export MINIMAX_API_KEY=your-key
```

## Ollama（本地部署）

```bash
export MATE_AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://127.0.0.1:11434
export OLLAMA_MODEL=qwen2.5:latest
```

## 查看配置

```bash
java -jar mate-cli.jar ai providers
```

此命令显示所有可用提供商及其当前配置状态。

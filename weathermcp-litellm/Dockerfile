FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN chmod +x start.sh
# Use LiteLLM's bundled model-cost map instead of fetching it on every cold start.
ENV LITELLM_LOCAL_MODEL_COST_MAP=True
# Default: everything in one container (Cloud Run). docker-compose.yml overrides
# `command` per service to run the MCP servers and the agent separately.
CMD ["./start.sh"]

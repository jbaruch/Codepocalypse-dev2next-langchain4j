# Docker Deployment

## Pre-built Docker Image

The Airline Loyalty Assistant is available as a Docker image on Docker Hub:

**Image**: `jbaruchs/codepocalypse-airline-assistant:latest`

This image includes:
- ✅ Quarkus application with Kotlin
- ✅ LangChain4j AI integration with OpenAI
- ✅ **MCP (Model Context Protocol) server** with airline loyalty program tools
- ✅ Web UI for chatting with the assistant

## Running the Docker Image

### Quick Start

```bash
docker run -p 8080:8080 \
  -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-openai-api-key \
  jbaruchs/codepocalypse-airline-assistant:latest
```

Then open http://localhost:8080 in your browser.

### With Environment Variables

```bash
docker run -p 8080:8080 \
  -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-openai-api-key \
  -e QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_MODEL_NAME=gpt-4o-mini-2024-07-18 \
  jbaruchs/codepocalypse-airline-assistant:latest
```

### Using Docker Compose

Create a `docker-compose.yml`:

```yaml
version: '3.8'

services:
  airline-assistant:
    image: jbaruchs/codepocalypse-airline-assistant:latest
    ports:
      - "8080:8080"
    environment:
      - QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=${OPENAI_API_KEY}
      - QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_MODEL_NAME=gpt-4o-mini-2024-07-18
```

Then run:

```bash
export OPENAI_API_KEY=your-openai-api-key
docker compose up
```

## MCP Server Access

The Docker image exposes an MCP server at:

**Endpoint**: `http://localhost:8080/mcp/sse`

You can connect external MCP clients to this endpoint to use the airline loyalty program tools.

### Available MCP Tools

1. **`getDeltaMedallionQualification`** - Fetches current Delta SkyMiles Medallion qualification requirements
2. **`getUnitedPremierQualification`** - Fetches current United MileagePlus Premier qualification requirements  
3. **`compareAirlinePrograms`** - Compares qualification requirements between Delta and United

### Testing the MCP Server

You can test the MCP server using the [MCP Inspector](https://github.com/modelcontextprotocol/inspector):

```bash
npx @modelcontextprotocol/inspector http://localhost:8080/mcp/sse
```

## Building Your Own Image

If you want to build the image locally:

```bash
# Package the application
./mvnw clean package -DskipTests

# Build the Docker image
docker build -f src/main/docker/Dockerfile.jvm -t my-airline-assistant:latest .

# Run it
docker run -p 8080:8080 -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-key my-airline-assistant:latest
```

## Configuration

The following environment variables can be configured:

| Variable | Description | Default |
|----------|-------------|---------|
| `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` | OpenAI API key (required) | - |
| `QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_MODEL_NAME` | OpenAI model to use | `gpt-4o-mini-2024-07-18` |
| `QUARKUS_LANGCHAIN4J_OPENAI_CHAT_MODEL_TEMPERATURE` | Model temperature | `0.7` |
| `QUARKUS_LANGCHAIN4J_CHAT_MEMORY_MAX_MESSAGES` | Message history size | `20` |

## Available Tags

- `latest` - Most recent build
- `mcp-stage-04` - Stage 04 release with MCP integration

## Image Details

- **Base Image**: `registry.access.redhat.com/ubi8/openjdk-21:1.23`
- **Java Version**: OpenJDK 21
- **Architecture**: linux/amd64
- **Port**: 8080
- **Size**: ~200MB (compressed layers)

## Troubleshooting

### Container won't start
- Ensure you've provided the `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` environment variable
- Check container logs: `docker logs <container-id>`

### Can't access the application
- Verify port 8080 is not in use: `lsof -i :8080`
- Ensure port mapping is correct: `-p 8080:8080`

### MCP tools not working
- Check the OpenAI API key is valid
- Verify network connectivity (MCP tools fetch data from airline websites)
- Increase timeout if needed: `-e QUARKUS_LANGCHAIN4J_MCP_AIRLINE_TOOLS_TOOL_EXECUTION_TIMEOUT=60s`

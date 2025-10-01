# Airline Loyalty Assistant

A demo application showcasing AI-powered airline loyalty program assistance using Quarkus, Kotlin, and LangChain4j with Model Context Protocol (MCP).

## Features

- 🤖 AI-powered question answering about airline loyalty programs
- ✈️ Real-time information from Delta and United websites via MCP tools
- ⚙️ **MCP Server** - Exposes airline loyalty program tools via Model Context Protocol
- 🎨 Simple, clean web UI with server-side rendering
- ⚡ Fast startup and hot reload with Quarkus
- 🐳 **Docker ready** - Pre-built images available on Docker Hub
- 🔧 Built with modern technology: Kotlin + Quarkus + LangChain4j + MCP

## Quick Start with Docker 🐳

The fastest way to run the application is using Docker:

```bash
docker run -p 8080:8080 \
  -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-openai-api-key \
  jbaruchs/codepocalypse-airline-assistant:latest
```

Then open http://localhost:8080 in your browser.

**See [DOCKER.md](DOCKER.md) for complete Docker deployment instructions.**

## Prerequisites (for local development)

- Java 21+
- Maven 3.9+ (or use the included wrapper)
- OpenAI API key (get one at [OpenAI Platform](https://platform.openai.com/api-keys))

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/jbaruch/Codepocalypse-dev2next-langchain4j.git
   cd Codepocalypse-dev2next-langchain4j
   ```

2. **Configure your OpenAI API key**

   Create a `.env` file in the project root (this file is git-ignored):

   ```bash
   cp .env.example .env
   ```

   Edit `.env` and replace the placeholder with your actual OpenAI API key:

   ```bash
   OPENAI_API_KEY=sk-proj-your-actual-openai-api-key-here
   ```

   > **Note:** The `.env` file is automatically loaded by Quarkus and is included in `.gitignore` to prevent accidentally committing your API keys.

## Running the Application

### Development Mode

Run the application in dev mode with live coding enabled:

```bash
./mvnw compile quarkus:dev
```

The application will be available at: <http://localhost:8080>

> **Note:** Quarkus Dev UI is available at <http://localhost:8080/q/dev/>

### Testing the Application

1. Open your browser to <http://localhost:8080>
2. Enter a question about airline loyalty programs, such as:
   - "How can I earn elite status?"
   - "What are the benefits of Gold tier membership?"
   - "Can I transfer miles to family members?"
3. Click "Ask Assistant" and wait for the AI response

## Architecture

This application follows the Quarkus LangChain4j programming model with MCP integration:

- **`AirlineLoyaltyAssistant.kt`** - AI Service interface with `@RegisterAiService` and `@McpToolBox` annotations
- **`AirlineMcpTools.kt`** - MCP server tools for fetching real-time airline data
- **`AssistantController.kt`** - REST controller handling web requests
- **`index.html`** - Qute template for server-side rendering
- **`application.properties`** - Configuration for OpenAI, MCP, and Quarkus

### MCP Server

The application exposes an MCP server at `/mcp/sse` that provides airline loyalty program tools:

- `getDeltaMedallionQualification` - Fetches Delta SkyMiles qualification requirements
- `getUnitedPremierQualification` - Fetches United MileagePlus qualification requirements
- `compareAirlinePrograms` - Compares both programs

You can connect external MCP clients to `http://localhost:8080/mcp/sse` to use these tools.

## Demo Stages

This project is organized into branches for different demo stages:

- `stage-00-init` - Initial project setup
- `stage-01-basic-chat` - Basic chatbot with conversation memory
- `stage-02-tools` - Added external tool integration
- `stage-03-rag` - RAG implementation with document loading
- `stage-04-mcp` - Model Context Protocol integration (current)

Switch between stages using:

```bash
git checkout stage-04-mcp
```

## Packaging and Running the Application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/codepocalypse-langchain4j-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- LangChain4j OpenAI ([guide](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)): Provides the basic integration with LangChain4j

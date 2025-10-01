# Airline Loyalty Assistant

A demo application showcasing AI-powered airline loyalty program assistance using Quarkus, Kotlin, and LangChain4j.

## Features

- 🤖 AI-powered question answering about airline loyalty programs
- ✈️ Information about miles, rewards, and elite status
- 🎨 Simple, clean web UI with server-side rendering
- ⚡ Fast startup and hot reload with Quarkus
- 🔧 Built with modern technology: Kotlin + Quarkus + LangChain4j

## Prerequisites

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
   
   Create a `.env` file in the project root:
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` and add your OpenAI API key:
   ```
   OPENAI_API_KEY=sk-your-actual-api-key-here
   ```
   
   Or export it directly:
   ```bash
   export OPENAI_API_KEY=sk-your-actual-api-key-here
   ```

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

This application follows the Quarkus LangChain4j programming model:

- **`AirlineLoyaltyAssistant.kt`** - AI Service interface with `@RegisterAiService` annotation
- **`AssistantController.kt`** - REST controller handling web requests
- **`index.html`** - Qute template for server-side rendering
- **`application.properties`** - Configuration for OpenAI and Quarkus

## Demo Stages

This project is organized into branches for different demo stages:

- `stage-00-init` - Initial project setup
- `stage-01-basic-chat` - Basic chatbot (current)
- Future stages will add: tools, RAG, memory, etc.

Switch between stages using:

```bash
git checkout stage-01-basic-chat
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

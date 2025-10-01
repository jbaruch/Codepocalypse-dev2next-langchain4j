# Stage 00: Project Setup Instructions

## Objective
Set up a minimal Quarkus + Kotlin + LangChain4j project with proper tooling and configuration.

## Prerequisites
- Java 21+ installed
- Maven 3.9+ installed
- Git configured
- GitHub account with repository access
- API key for LLM provider (OpenAI, Ollama, etc.)

## Step 1: Initialize Quarkus Project

Use Quarkus CLI or Maven archetype to create project:

```bash
mvn io.quarkus:quarkus-maven-plugin:3.28.1:create \
    -DprojectGroupId=dev2next \
    -DprojectArtifactId=codepocalypse-langchain4j \
    -Dextensions="resteasy-reactive,kotlin,quarkus-config-yaml" \
    -DnoCode
```

**Key Points:**
- Use Quarkus 3.28.1 or later
- Include `kotlin` extension for Kotlin support
- Include `resteasy-reactive` for REST endpoints
- Include `quarkus-config-yaml` for .env file support
- Use `-DnoCode` to skip example code generation

## Step 2: Configure Kotlin in pom.xml

Add Kotlin dependencies and plugin configuration:

```xml
<properties>
    <kotlin.version>2.1.0</kotlin.version>
    <quarkus.platform.version>3.28.1</quarkus.platform.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlin</groupId>
        <artifactId>kotlin-stdlib-jdk8</artifactId>
    </dependency>
</dependencies>

<build>
    <sourceDirectory>src/main/kotlin</sourceDirectory>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <version>${kotlin.version}</version>
            <configuration>
                <compilerPlugins>
                    <plugin>all-open</plugin>
                </compilerPlugins>
                <pluginOptions>
                    <option>all-open:annotation=jakarta.ws.rs.Path</option>
                    <option>all-open:annotation=jakarta.enterprise.context.ApplicationScoped</option>
                </pluginOptions>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Critical Configuration:**
- Use `all-open` plugin for Quarkus proxy support
- Configure annotations for `@Path` and `@ApplicationScoped`
- Set source directory to `src/main/kotlin`

## Step 3: Add Quarkus LangChain4j Extension

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-openai</artifactId>
    <version>0.21.0</version>
</dependency>
```

**Important Notes:**
- Use `io.quarkiverse.langchain4j` (Quarkus extension)
- NOT `dev.langchain4j` (vanilla LangChain4j - won't work with Quarkus)
- Choose provider-specific artifact (openai, ollama, etc.)
- Check https://docs.quarkiverse.io/quarkus-langchain4j/dev/ for latest version

## Step 4: Add Qute Templating

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-qute</artifactId>
</dependency>
```

## Step 5: Configure Secret Management

### Create .env File
Create `.env` in project root (NOT tracked by git):

```bash
OPENAI_API_KEY=sk-your-actual-api-key-here
```

### Create .env.example Template
Create `.env.example` (safe to commit):

```bash
# OpenAI API Key
# Get your key from: https://platform.openai.com/api-keys
OPENAI_API_KEY=sk-your-api-key-here
```

### Update .gitignore
Ensure `.env` is in `.gitignore`:

```gitignore
# Local environment files
.env
.env.local
.env.*.local
```

**Why .env over environment variables:**
- Easier to manage multiple keys
- Better local development experience
- Clear separation of secrets from code
- Team can copy .env.example and add their keys

## Step 6: Create Basic application.properties

Create `src/main/resources/application.properties`:

```properties
# Application name
quarkus.application.name=codepocalypse-langchain4j

# HTTP configuration
quarkus.http.port=8080

# Quarkus LangChain4j OpenAI Configuration
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
quarkus.langchain4j.openai.chat-model.temperature=0.7
quarkus.langchain4j.openai.timeout=30s

# Enable request/response logging for debugging
quarkus.langchain4j.log-requests=true
quarkus.langchain4j.log-responses=true

# Logging
quarkus.log.level=INFO
quarkus.log.category."io.quarkiverse.langchain4j".level=DEBUG
```

**Configuration Notes:**
- Use `${OPENAI_API_KEY}` to read from .env file
- Set appropriate model (gpt-4o-mini for cost-effective demos)
- Enable logging for debugging LangChain4j interactions
- Configure reasonable timeout (30s)

## Step 7: Project Structure Setup

Create directory structure:

```
src/main/
  kotlin/
    dev2next/
      langchain4j/
        (future: controller and service files)
  resources/
    templates/
      (future: Qute templates)
    application.properties
```

## Step 8: Initialize Git Repository

```bash
git init
git add .
git commit -m "feat: initial project setup - Quarkus + Kotlin + LangChain4j"

# Create remote and push
git remote add origin https://github.com/yourusername/your-repo.git
git branch -M main
git push -u origin main

# Create stage branch
git checkout -b stage-00-init
git push -u origin stage-00-init
git checkout main
```

## Step 9: Verify Setup

Test the basic setup:

```bash
# Build project
./mvnw clean compile

# Should see:
# [INFO] BUILD SUCCESS
```

**Do NOT run `./mvnw quarkus:dev` yet** - there's no application code to start.

## Step 10: Create Project Documentation

### Create README.md

Include:
- Project description
- Prerequisites
- Environment setup instructions (.env file)
- How to run the application
- Project stages overview
- Technology stack

### Create .github/copilot-instructions.md

Document:
- Project architecture principles (simplicity first)
- Quarkus LangChain4j vs vanilla LangChain4j differences
- Coding guidelines (Kotlin, CDI patterns)
- Common pitfalls to avoid
- Development workflow

## Verification Checklist

Before proceeding to Stage 01, verify:

- ✅ Project builds successfully (`./mvnw clean compile`)
- ✅ Kotlin compilation works
- ✅ pom.xml has all required dependencies
- ✅ `.env` file created and in `.gitignore`
- ✅ `.env.example` template created
- ✅ `application.properties` configured for LangChain4j
- ✅ Directory structure created
- ✅ Git repository initialized and pushed
- ✅ Documentation complete (README, copilot-instructions)

## Common Issues & Solutions

### Issue: Kotlin classes not opening for proxies
**Solution**: Ensure `all-open` plugin configured with correct annotations

### Issue: Cannot read OPENAI_API_KEY
**Solution**: Verify `quarkus-config-yaml` dependency added for .env support

### Issue: Wrong LangChain4j package imports
**Solution**: Use `io.quarkiverse.langchain4j.*` NOT `dev.langchain4j.*`

## Next Steps

Proceed to **Stage 01: Basic Airline Loyalty Assistant** to implement:
- AI Service with `@RegisterAiService`
- REST Controller with form handling
- Qute template for UI
- Basic chat functionality

## Key Learnings

1. **Use Quarkus LangChain4j extension** - Different from vanilla LangChain4j
2. **Configure .env files early** - Better than environment variables
3. **Enable debug logging** - Essential for troubleshooting AI integrations
4. **Document architecture decisions** - Helps maintain consistency
5. **Create stage branches** - Allows safe experimentation and rollback

# Airline Loyalty Assistant - Copilot Instructions

## Project Overview
This is a **LOCAL DEMO APPLICATION** for an AI-powered airline loyalty assistant. The primary goal is **SIMPLICITY** over production best practices. This is a learning and demonstration tool, not a production application.

## Core Technology Stack
- **Language**: Kotlin (primary language for all new code)
- **Build Tool**: Maven with wrapper
- **Framework**: Quarkus (minimal feature set)
- **AI Integration**: Quarkus LangChain4j extension (NOT vanilla LangChain4j - uses Quarkus CDI and programming model)
- **Templating**: Quarkus Qute for server-side rendering (NO separate frontend/API architecture)

### CRITICAL: Quarkus LangChain4j vs Vanilla LangChain4j
**We are using the Quarkus LangChain4j extension (`io.quarkiverse.langchain4j`), which has a DIFFERENT programming model:**
- Uses Quarkus CDI/Arc for dependency injection
- Uses `@RegisterAiService` annotation for AI services
- Configuration via `application.properties` (e.g., `quarkus.langchain4j.*`)
- Different patterns for model initialization and service creation
- **Official Documentation**: https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html
- Do NOT follow vanilla LangChain4j patterns - they will not work with Quarkus

## Architecture Principles

### Simplicity First
- Maximum 5-6 Kotlin files for core implementation
- Single controller for all endpoints
- Single service layer for business logic
- Server-side rendering with Qute templates - NO complex JavaScript
- When in doubt, choose simpler over "cleaner" architecture
- Deliberately omit production features that add complexity without demo value

### What NOT to Implement
- Health checks, metrics, or observability endpoints
- CORS configuration (server-side rendering = no cross-origin requests)
- Complex error mappers or JSON error handling
- Client-side API interaction or JavaScript frameworks
- Advanced security features
- Multiple controllers or over-architected layers

## Quarkus LangChain4j Integration Guidelines

### Documentation-First Approach
When implementing Quarkus LangChain4j features:
1. **Primary Reference**: Always consult https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html
2. **Use Context7 MCP**: Query for Quarkus LangChain4j documentation using `#mcp_context7-mcp_resolve-library-id` with "quarkus-langchain4j"
3. **Verify APIs**: Package structures and method signatures change between versions - always verify current API
4. **Check Quarkus Examples**: Reference Quarkus LangChain4j samples, NOT vanilla LangChain4j examples
5. **Model Agnostic**: Design to work with any LLM provider (OpenAI, Ollama, etc.)

### Quarkus-Specific Integration Patterns
- **AI Services**: Use `@RegisterAiService` annotation with interface definitions
- **Dependency Injection**: Use `@Inject` to inject AI services (Quarkus CDI/Arc)
- **Configuration**: Use `application.properties` with `quarkus.langchain4j.*` prefix
- **Message Handling**: SystemMessage via `@SystemMessage`, UserMessage via method parameters
- **Model Configuration**: Configured declaratively in `application.properties`, not programmatically
- **Response Handling**: Return types from AI service methods (String, AiMessage, etc.)
- **Memory/Context**: Use `@MemoryId` and `@UserMessage` annotations

### Common Mistakes to Avoid
1. **DON'T**: Manually instantiate models (e.g., `OpenAiChatModel.builder()...`)
   **DO**: Configure in `application.properties` and inject via `@Inject`
2. **DON'T**: Use vanilla LangChain4j examples directly
   **DO**: Follow Quarkus LangChain4j patterns with annotations and CDI
3. **DON'T**: Import from `dev.langchain4j.*` packages for core functionality
   **DO**: Import from `io.quarkiverse.langchain4j.*` and use Quarkus patterns

### Error Investigation
Build errors with Quarkus LangChain4j are usually:
1. Incorrect package imports (using vanilla instead of Quarkus extension)
2. Missing Quarkus-specific annotations (@RegisterAiService, @SystemMessage, etc.)
3. Attempting to use vanilla LangChain4j patterns instead of Quarkus CDI patterns
4. Missing configuration in application.properties
5. Missing dependencies in pom.xml

## Project Structure

### File Organization
```
src/main/
  kotlin/
    - Single controller (handles all endpoints)
    - Single service class (business logic)
    - Data models (simple, minimal)
  resources/
    templates/
      {ControllerName}/
        - index.html (main UI)
        - error.html (error pages)
    application.properties
```

### Dependency Management
- Use Context7 MCP to look up latest stable versions
- Prefer Quarkus BOM for dependency management
- Keep dependencies minimal - only what's needed for the demo

## Application Endpoints

### UI Endpoint
- `GET /` - Serves HTML UI with Qute templating

### Form Processing
- `POST /` - Accepts form data with `query` parameter
- Returns HTML response with AI-generated answer rendered in the page

### Error Handling
- Render errors directly in templates
- Server-side validation for input
- Clear user-facing error messages
- Console logging for debugging

## Configuration Strategy
- Use `application.properties` for configuration
- Support environment variables for API keys
- Externalize only what's necessary for the demo
- Document all required configuration in README

## Code Quality Guidelines
- Write idiomatic, concise Kotlin code
- Avoid unnecessary abstractions
- Keep it readable and maintainable
- Design for easy extensibility (future: RAG, local models, new endpoints)
- Comment complex AI integration logic

## Deliverables
1. **Backend**: Kotlin service with LangChain4j integration
2. **Templates**: Qute templates for UI rendering
3. **Build Files**: pom.xml and Maven wrapper
4. **README.md**: Setup, environment variables, how to run/test
5. **Demo-Ready**: Should start with `./mvnw quarkus:dev` and work immediately

## Development Workflow
1. Start dev mode: `./mvnw compile quarkus:dev`
2. Access Dev UI: http://localhost:8080/q/dev/
3. Hot reload enabled - changes apply immediately
4. Focus on making it WORK, not architecturally perfect

## Copilot Usage Tips
- Ask Copilot to check **Quarkus LangChain4j** docs (https://docs.quarkiverse.io/quarkus-langchain4j/dev/) via Context7 before implementing
- Always specify "use Quarkus LangChain4j extension with CDI" to avoid vanilla patterns
- Request simple, demo-focused solutions
- Specify "use Qute templates" for any UI work
- Request "minimal Maven pom.xml changes" for dependencies
- Use "check latest Quarkus documentation" for framework features
- When asking about LangChain4j, clarify "Quarkus LangChain4j" not "vanilla LangChain4j"

# RFC-022: API Documentation (mate-doc-starter)

| Field       | Value                                     |
|-------------|-------------------------------------------|
| **RFC**     | 022                                       |
| **Title**   | Smart-Doc API Documentation Starter       |
| **Status**  | Draft                                     |
| **Created** | 2026-04-11                                |
| **Module**  | mate-doc-starter (new starter)            |
| **Package** | vip.mate.starter.doc                      |

---

## 1. Overview

Provides automated API documentation generation using Smart-Doc. Unlike Swagger/SpringDoc, Smart-Doc analyzes Java source code and Javadoc comments directly -- no runtime annotations needed. This keeps business code clean while producing comprehensive documentation.

Features:
- Zero-annotation API documentation from Javadoc
- HTML, Markdown, and Postman collection output formats
- Dubbo RPC interface documentation
- Auto-configuration with sensible defaults
- Maven plugin integration for CI/CD doc generation

---

## 2. Module Structure

```
mate-starters/mate-doc-starter/
  pom.xml
  src/main/java/vip/mate/starter/doc/
    config/
      SmartDocAutoConfiguration.java
      SmartDocProperties.java
  src/main/resources/
    META-INF/
      spring/
        org.springframework.boot.autoconfigure.AutoConfiguration.imports
    smart-doc-config-template.json
```

---

## 3. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-doc-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Doc Starter - Smart-Doc API documentation</description>

    <dependencies>
        <!-- Smart-Doc (provided scope - only needed at build time) -->
        <dependency>
            <groupId>com.ly.smart-doc</groupId>
            <artifactId>smart-doc</artifactId>
            <version>${smart-doc.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Spring Boot Autoconfigure -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>

        <!-- Configuration Processor (for IDE hints) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

---

## 4. Source Code

### 4.1 SmartDocProperties.java

```java
package vip.mate.starter.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Smart-Doc integration.
 * <p>
 * These properties are used to generate the {@code smart-doc-config.json}
 * at build time or provide defaults for the Maven plugin.
 *
 * <pre>{@code
 * mate:
 *   doc:
 *     server-url: http://localhost:9020
 *     all-in-one: true
 *     out-path: docs/api
 *     package-filters: vip.mate.auth.trigger.controller
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "mate.doc")
public class SmartDocProperties {

    /**
     * Whether doc generation is enabled.
     * Default: true.
     */
    private boolean enabled = true;

    /**
     * Server URL for API documentation (used as base URL in generated docs).
     * Default: http://localhost:8080.
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * Whether to merge all APIs into a single document.
     * Default: true.
     */
    private boolean allInOne = true;

    /**
     * Output directory for generated documentation (relative to project root).
     * Default: docs/api.
     */
    private String outPath = "docs/api";

    /**
     * Package filter patterns for scanning controllers.
     * If empty, scans all packages.
     */
    private List<String> packageFilters = new ArrayList<>();

    /**
     * Whether to generate Dubbo RPC documentation.
     * Default: false.
     */
    private boolean dubboDocEnabled = false;

    /**
     * Package filter for Dubbo RPC interfaces.
     */
    private List<String> dubboPackageFilters = new ArrayList<>();

    /**
     * Project name shown in documentation header.
     * Default: MateCloud API.
     */
    private String projectName = "MateCloud API";

    /**
     * Whether to display the request/response example in docs.
     * Default: true.
     */
    private boolean showRequestExample = true;

    /**
     * Whether to display the response body advice wrapper.
     * Default: true (wraps responses in Result<T>).
     */
    private boolean responseBodyAdvice = true;

    /**
     * Response body advice class name.
     * Default: vip.mate.base.result.Result.
     */
    private String responseBodyAdviceClassName = "vip.mate.base.result.Result";
}
```

### 4.2 SmartDocAutoConfiguration.java

```java
package vip.mate.starter.doc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Smart-Doc API documentation.
 * <p>
 * Smart-Doc is a build-time documentation tool, so this auto-configuration
 * primarily provides property binding for the {@code smart-doc-config.json}
 * template and exposes the config as a Spring bean for programmatic access.
 * <p>
 * Actual doc generation is done via the Maven plugin:
 * <pre>
 * mvn smart-doc:html          # Generate HTML docs
 * mvn smart-doc:markdown      # Generate Markdown docs
 * mvn smart-doc:postman       # Generate Postman collection
 * mvn smart-doc:rpc-html      # Generate Dubbo RPC docs (HTML)
 * </pre>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SmartDocProperties.class)
@ConditionalOnProperty(prefix = "mate.doc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmartDocAutoConfiguration {

    @Bean
    public SmartDocProperties smartDocProperties() {
        log.info("[SmartDoc] Smart-Doc auto-configuration loaded. " +
                "Use 'mvn smart-doc:html' to generate API documentation.");
        return new SmartDocProperties();
    }
}
```

---

## 5. AutoConfiguration Registration

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
vip.mate.starter.doc.config.SmartDocAutoConfiguration
```

---

## 6. smart-doc-config.json Template

Place at each service module root (e.g., `mate-auth/src/main/resources/smart-doc-config.json`):

### 6.1 REST API Config (mate-auth example)

```json
{
  "serverUrl": "http://localhost:9020",
  "isStrict": false,
  "allInOne": true,
  "outPath": "docs/api",
  "projectName": "MateCloud Auth Service API",
  "packageFilters": "vip.mate.auth.trigger.controller",
  "showAuthor": true,
  "createDebugPage": true,
  "style": "xt256",
  "coverOld": true,
  "requestExample": "true",
  "responseExample": "true",
  "displayActualType": true,
  "appToken": "",
  "responseBodyAdvice": {
    "className": "vip.mate.base.result.Result",
    "typeParameters": ["T"]
  },
  "requestHeaders": [
    {
      "name": "satoken",
      "type": "string",
      "desc": "Sa-Token authentication token",
      "value": "",
      "required": false,
      "since": "1.0.0",
      "pathPatterns": "/api/**",
      "excludePathPatterns": "/api/v1/auth/login,/api/v1/auth/sms/**,/api/v1/auth/captcha"
    }
  ],
  "errorCodeDictionaries": [
    {
      "title": "Error Codes",
      "enumClassName": "vip.mate.base.enums.ErrorCode",
      "codeField": "code",
      "descField": "message"
    }
  ],
  "revisionLogs": [
    {
      "version": "1.0.0",
      "revisionTime": "2026-04-11",
      "status": "create",
      "author": "MateCloud Team",
      "remarks": "Initial API documentation"
    }
  ]
}
```

### 6.2 Dubbo RPC Doc Config (mate-system example)

```json
{
  "serverUrl": "dubbo://127.0.0.1:20880",
  "isStrict": false,
  "allInOne": true,
  "outPath": "docs/rpc",
  "projectName": "MateCloud System Service RPC API",
  "packageFilters": "vip.mate.api.service",
  "showAuthor": true,
  "coverOld": true,
  "displayActualType": true,
  "rpcApiDependencies": [
    {
      "artifactId": "mate-api",
      "groupId": "vip.mate",
      "version": "1.0.0"
    }
  ],
  "rpcConsumerConfig": {
    "registryCenter": "nacos://127.0.0.1:8848"
  },
  "revisionLogs": [
    {
      "version": "1.0.0",
      "revisionTime": "2026-04-11",
      "status": "create",
      "author": "MateCloud Team",
      "remarks": "Initial RPC documentation"
    }
  ]
}
```

---

## 7. Maven Plugin Configuration

### 7.1 Root pom.xml Plugin Management

Add to root `pom.xml` `<pluginManagement>`:

```xml
<pluginManagement>
    <plugins>
        <!-- Smart-Doc Maven Plugin -->
        <plugin>
            <groupId>com.ly.smart-doc</groupId>
            <artifactId>smart-doc-maven-plugin</artifactId>
            <version>${smart-doc-maven-plugin.version}</version>
            <configuration>
                <configFile>./src/main/resources/smart-doc-config.json</configFile>
                <includes>
                    <!-- Include project modules for source analysis -->
                    <include>vip.mate:mate-base</include>
                    <include>vip.mate:mate-api</include>
                </includes>
                <excludes>
                    <!-- Exclude test dependencies -->
                    <exclude>junit:junit</exclude>
                    <exclude>org.mockito:.*</exclude>
                </excludes>
            </configuration>
            <executions>
                <execution>
                    <id>generate-html</id>
                    <goals>
                        <goal>html</goal>
                    </goals>
                    <phase>none</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</pluginManagement>
```

### 7.2 Per-Service Plugin Declaration

In each service's `pom.xml` (e.g., `mate-auth/pom.xml`):

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- Smart-Doc Plugin (inherited from parent pluginManagement) -->
        <plugin>
            <groupId>com.ly.smart-doc</groupId>
            <artifactId>smart-doc-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

---

## 8. Documentation Generation Commands

### 8.1 Generate HTML Documentation

```bash
# Generate for a specific service
cd mate-auth
mvn smart-doc:html

# Generate for all services from root
mvn smart-doc:html -pl mate-auth,mate-biz/mate-system
```

Output: `docs/api/index.html` (single-page HTML with all APIs)

### 8.2 Generate Markdown Documentation

```bash
mvn smart-doc:markdown -pl mate-auth
```

Output: `docs/api/AllInOne.md`

### 8.3 Generate Postman Collection

```bash
mvn smart-doc:postman -pl mate-auth
```

Output: `docs/api/postman.json` (importable into Postman)

### 8.4 Generate Dubbo RPC Documentation

```bash
mvn smart-doc:rpc-html -pl mate-biz/mate-system
```

Output: `docs/rpc/index.html`

### 8.5 Generate All Formats at Once

Add a Maven profile in root `pom.xml`:

```xml
<profiles>
    <profile>
        <id>generate-docs</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>com.ly.smart-doc</groupId>
                    <artifactId>smart-doc-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>html</id>
                            <goals><goal>html</goal></goals>
                            <phase>compile</phase>
                        </execution>
                        <execution>
                            <id>markdown</id>
                            <goals><goal>markdown</goal></goals>
                            <phase>compile</phase>
                        </execution>
                        <execution>
                            <id>postman</id>
                            <goals><goal>postman</goal></goals>
                            <phase>compile</phase>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Usage:

```bash
mvn compile -Pgenerate-docs -pl mate-auth
```

---

## 9. Javadoc Conventions for Smart-Doc

Smart-Doc reads standard Javadoc. Follow these conventions for best results:

### 9.1 Controller Documentation

```java
package vip.mate.auth.trigger.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.auth.application.command.SmsLoginCommand;
import vip.mate.base.result.Result;

import java.util.Map;

/**
 * SMS Authentication API
 *
 * Provides SMS-based login endpoints for mobile verification.
 *
 * @author MateCloud Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth/sms")
@RequiredArgsConstructor
public class SmsAuthController {

    /**
     * Send SMS verification code
     *
     * Sends a 6-digit verification code to the specified mobile number.
     * Rate limited to 1 request per minute per mobile, 10 requests per hour per IP.
     *
     * @param command mobile number|required
     * @return success indicator
     * @apiNote This endpoint does not require authentication
     * @since 1.0.0
     */
    @PostMapping("/send")
    public Result<Void> sendCode(@Valid @RequestBody SmsSendCommand command) {
        // ...
    }

    /**
     * SMS code login
     *
     * Verifies the SMS code and creates a new session.
     * If the mobile is not registered, a new user account is created automatically.
     *
     * @param command mobile + code|required
     * @return token information including token value and token name
     * @apiNote This endpoint does not require authentication
     * @since 1.0.0
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody SmsLoginCommand command) {
        // ...
    }
}
```

### 9.2 Request Object Documentation

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * SMS login request.
 */
@Data
public class SmsLoginCommand {

    /**
     * Mobile phone number (11 digits starting with 1).
     *
     * @mock 13800138000
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String mobile;

    /**
     * 6-digit SMS verification code.
     *
     * @mock 123456
     */
    @NotBlank(message = "SMS code is required")
    @Pattern(regexp = "^\\d{6}$")
    private String code;
}
```

### 9.3 Response Wrapper Documentation

The `Result<T>` class in `mate-base` should already have Javadoc:

```java
package vip.mate.base.result;

import lombok.Data;

/**
 * Unified API response wrapper.
 *
 * @param <T> the data type
 */
@Data
public class Result<T> {

    /**
     * Response code. 0 = success, non-zero = error.
     */
    private int code;

    /**
     * Response message.
     */
    private String msg;

    /**
     * Response data payload.
     */
    private T data;
}
```

---

## 10. Dubbo RPC Interface Documentation

Smart-Doc supports Dubbo RPC doc generation from interface Javadoc:

```java
package vip.mate.api.service;

import vip.mate.api.dto.UserInfoDTO;

/**
 * User RPC Service
 *
 * Provides user data retrieval and management via Dubbo RPC.
 * Implemented by mate-system, consumed by mate-auth and other services.
 *
 * @author MateCloud Team
 * @dubbo
 * @since 1.0.0
 */
public interface IRpcUserService {

    /**
     * Find user by mobile number.
     *
     * @param mobile 11-digit mobile phone number|required
     * @return user information including roles and permissions, or null if not found
     * @since 1.0.0
     */
    UserInfoDTO findByMobile(String mobile);

    /**
     * Find user by ID.
     *
     * @param userId the user's unique ID|required
     * @return user information or null if not found
     * @since 1.0.0
     */
    UserInfoDTO findById(Long userId);

    /**
     * Create a new user by mobile number (auto-registration for SMS login).
     *
     * @param mobile 11-digit mobile phone number|required
     * @return newly created user information with default role USER
     * @since 1.0.0
     */
    UserInfoDTO createByMobile(String mobile);
}
```

---

## 11. CI/CD Integration

### 11.1 GitHub Actions / CI Step

```yaml
# In .github/workflows/ci.yml
- name: Generate API Documentation
  run: |
    mvn smart-doc:html -pl mate-auth,mate-biz/mate-system --no-transfer-progress
    mvn smart-doc:rpc-html -pl mate-biz/mate-system --no-transfer-progress

- name: Upload API Docs Artifact
  uses: actions/upload-artifact@v4
  with:
    name: api-docs
    path: |
      mate-auth/docs/api/
      mate-biz/mate-system/docs/api/
      mate-biz/mate-system/docs/rpc/
```

### 11.2 Makefile Target

```makefile
.PHONY: docs docs-html docs-markdown docs-postman docs-rpc

docs: docs-html docs-markdown docs-postman docs-rpc

docs-html:
	mvn smart-doc:html -pl mate-auth,mate-biz/mate-system

docs-markdown:
	mvn smart-doc:markdown -pl mate-auth,mate-biz/mate-system

docs-postman:
	mvn smart-doc:postman -pl mate-auth,mate-biz/mate-system

docs-rpc:
	mvn smart-doc:rpc-html -pl mate-biz/mate-system
```

---

## 12. Generated Output Examples

After running `mvn smart-doc:html`, the generated HTML includes:

- **API overview** with project name and version
- **Endpoint list** grouped by controller
- **For each endpoint:**
  - HTTP method + URL path
  - Request parameters table (name, type, required, description, default)
  - Request body JSON example (using `@mock` values from Javadoc)
  - Response body JSON example
  - Error code table
- **Global request headers** (e.g., satoken)
- **Error code dictionary** from ErrorCode enum
- **Revision history**

---

## 13. Testing Checklist

- [ ] `mvn smart-doc:html -pl mate-auth` generates HTML in `docs/api/`
- [ ] `mvn smart-doc:markdown -pl mate-auth` generates Markdown
- [ ] `mvn smart-doc:postman -pl mate-auth` generates importable Postman collection
- [ ] `mvn smart-doc:rpc-html -pl mate-biz/mate-system` generates Dubbo RPC docs
- [ ] Generated docs include all controller endpoints with correct parameters
- [ ] `@mock` values appear in request/response examples
- [ ] Response body is wrapped in `Result<T>` via responseBodyAdvice
- [ ] Error code dictionary lists all ErrorCode enum values
- [ ] satoken header appears for authenticated endpoints only
- [ ] `mvn compile -Pgenerate-docs` generates all formats in one pass

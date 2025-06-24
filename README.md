# log-analyzer-client

log-analyzer-client is a secure and lightweight Java library designed to integrate with the LogAnalyzer platform.
It performs validation checks on your application's identity before allowing logs to be pushed to Kafka — ensuring only authorized and properly configured apps can contribute to your centralized logging system.

## 📦 Maven Setup

### 📚 Repository
To include this library in your Maven project, add the following repository configuration to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/koushikruidas/log-analyzer-client</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### ➕ Add Dependency
```xml
<dependency>
    <groupId>com.poinciana</groupId>
    <artifactId>log-analyzer-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 🚀 Getting Started

### ✅ Prerequisites
- Java 11 or higher
- Spring Boot framework
- A valid configuration file (`log4j2.xml` or `.properties`)
- Application details registered with the loganalyzer-admin service

### 🛠️ Step 1: Specify Config Path
In your `application.properties` file, add the following property to point to your configuration file:

```properties
log.analyzer.config.path=src/main/resources/log4j2.xml
# OR
log.analyzer.config.path=src/main/resources/log-analyzer.properties
```

This property tells the library where to find the configuration file from which it will extract the necessary details:
- application.name
- organization.name
- apiKey
- kafka.topic
- log.analyzer.api.url

### 📝 Step 2: Create a Configuration File
You can use either an XML or a `.properties` file to define the required fields.

#### Option 1: log4j2.xml
```xml
<Properties>
    <Property name="application.name">your-app-name</Property>
    <Property name="organization.name">your-org-name</Property>
    <Property name="apiKey">your-api-key</Property>
    <Property name="kafka.topic">your-kafka-topic</Property>
    <Property name="log.analyzer.api.url">https://loganalyzer-admin.company.com/api/validate</Property>
</Properties>
```

#### Option 2: log-analyzer.properties
```properties
application.name=your-app-name
organization.name=your-org-name
apiKey=your-api-key
kafka.topic=your-kafka-topic
log.analyzer.api.url=https://loganalyzer-admin.company.com/api/validate
```

### 🧩 Step 3: Enable Component Scanning in Spring Boot
Add the `@ComponentScan` annotation to your main Spring Boot application class:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.poinciana.log_analyzer_client"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

✅ That’s all you need! The library will automatically run validation when your application starts — no need to call any methods manually.

## 🔐 How It Works
- The library reads the configuration file specified by `log.analyzer.config.path`.
- It extracts all required metadata fields (e.g., `application.name`, `organization.name`, `apiKey`, `kafka.topic`, `log.analyzer.api.url`).
- Using the `@PostConstruct` annotation, it automatically sends a validation request to the loganalyzer-admin service when your application starts up.
- If the validation fails, your application is blocked from sending logs to Kafka, preventing unauthorized log data.

## ✨ Features
- ✅ Automatic validation at startup via `@PostConstruct`
- 🔒 Blocks unauthorized logging attempts
- 🧾 Supports both XML (`log4j2.xml`) and `.properties` configurations
- 📦 Lightweight — minimal dependencies and configuration
- 🧠 Easy Spring Boot integration with just `@ComponentScan`

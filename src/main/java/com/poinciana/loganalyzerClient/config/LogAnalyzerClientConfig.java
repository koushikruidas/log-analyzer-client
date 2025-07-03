package com.poinciana.loganalyzerClient.config;

import com.poinciana.loganalyzerClient.model.ApiKeyResponseDTO;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

@Slf4j
@Getter
@Component
public class LogAnalyzerClientConfig {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${log.analyzer.config.path}")
    private String logConfigPath;

    @Value("${log.analyzer.service.url}")
    private String logAnalyzerUrl;

    private String appName;
    private String orgName;
    private String apiKey;
    private String kafkaTopic;

    @PostConstruct
    public void loadLogConfig() {
        try {
            File logConfigFile = new File(logConfigPath);
            if (!logConfigFile.exists()) {
                log.warn("Log config file not found at: {}. Skipping.", logConfigPath);
                return;
            }

            if (logConfigPath.endsWith(".properties")) {
                loadFromPropertiesFile(logConfigFile);
            } else if (logConfigPath.endsWith(".xml")) {
                loadFromXmlConfig(logConfigFile);
            } else {
                log.error("Unsupported log configuration format: {}", logConfigPath);
            }
            validateApiKey();
            log.info("LogAnalyzerClientConfig Loaded: app={}, org={}, topic={}", appName, orgName, kafkaTopic);
        } catch (Exception e) {
            log.error("Failed to load log configuration!", e);
            System.exit(1);  // Forcefully stop the application
        }
    }

    private void loadFromPropertiesFile(File file) throws IOException {
        Properties props = new Properties();
        props.load(file.toURI().toURL().openStream());

        this.logAnalyzerUrl = props.getProperty("log.analyzer.api.url");
        this.appName = props.getProperty("application.name");
        this.orgName = props.getProperty("organization.name");
        this.apiKey = props.getProperty("apiKey");
        this.kafkaTopic = props.getProperty("kafka.topic");
    }

    private void loadFromXmlConfig(File file) {
        try {
            Configuration cfg = ((LoggerContext) LogManager.getContext(false)).getConfiguration();
            this.kafkaTopic   = cfg.getStrSubstitutor().replace("${kafka.topic}");
            this.logAnalyzerUrl  = cfg.getStrSubstitutor().replace("${log.analyzer.api.url}");
            this.appName      = cfg.getStrSubstitutor().replace("${application.name}");
            this.orgName      = cfg.getStrSubstitutor().replace("${organization.name}");
            this.apiKey       = cfg.getStrSubstitutor().replace("${apiKey}");
            log.info("XML Config Loaded: app={}, org={}, topic={}", appName, orgName, kafkaTopic);
        } catch (Exception e) {
            log.error("Failed to parse XML configuration!", e);
        }
    }

    private String getXmlProperty(Document doc, String propertyName) {
        NodeList nodes = doc.getElementsByTagName("Property");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (element.getAttribute("name").equals(propertyName)) {
                return element.getTextContent();
            }
        }
        return null;
    }

    private void validateApiKey() {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(logAnalyzerUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("appName", appName)
                    .queryParam("orgName", orgName);

            URI apiUri = builder.build().toUri();
            ResponseEntity<ApiKeyResponseDTO> response =
                    restTemplate.getForEntity(apiUri, ApiKeyResponseDTO.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Invalid API Key! Cannot start the application.");
            }

            assertApiKeyResponseMatches(response);

            log.info("API Key is valid for app: {}", appName);
        } catch (Exception e) {
            log.error("API Key validation failed! Shutting down application.", e);
            System.exit(1);    // Forcefully stop the application
        }
    }

    private void assertApiKeyResponseMatches(ResponseEntity<ApiKeyResponseDTO> response) {
        ApiKeyResponseDTO responseBody = response.getBody();
        if (!apiKey.equals(responseBody.getApiKey())) {
            throw new IllegalStateException("API Key validation failed! ApiKey mismatch.");
        }
        if (!appName.equals(responseBody.getApplicationName())) {
            throw new IllegalStateException("API Key validation failed! Application Name mismatch.");
        }
        if (!orgName.equals(responseBody.getOrganizationName())) {
            throw new IllegalStateException("API Key validation failed! Organization Name mismatch.");
        }
        if (!kafkaTopic.equals(responseBody.getKafkaTopic())) {
            throw new IllegalStateException("API Key validation failed! Kafka Topic mismatch.");
        }
    }
}

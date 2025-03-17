package com.poinciana.log_analyzer_client.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

        this.appName = props.getProperty("appName");
        this.orgName = props.getProperty("orgName");
        this.apiKey = props.getProperty("apiKey");
        this.kafkaTopic = props.getProperty("topic");
    }

    private void loadFromXmlConfig(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            this.logAnalyzerUrl = getXmlProperty(doc, "log.analyzer.api.url");
            this.appName = getXmlProperty(doc, "application.name");
            this.orgName = getXmlProperty(doc, "organization.name");
            this.apiKey = getXmlProperty(doc, "apiKey");
            this.kafkaTopic = getXmlProperty(doc, "default.kafka.topic");
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
            ResponseEntity<String> response = restTemplate.getForEntity(apiUri, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("🚨 Invalid API Key! Cannot start the application.");
            }

            log.info("API Key is valid for app: {}", appName);
        } catch (Exception e) {
            log.error("API Key validation failed! Shutting down application.", e);
            System.exit(1);    // Forcefully stop the application
        }
    }
}

package com.poinciana.loganalyzerClient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponseDTO {

    private String organizationName;
    private String applicationName;
    private String apiKey;
    private String kafkaTopic;
    private String elasticIndex;
    private boolean active;
}
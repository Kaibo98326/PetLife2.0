package com.petlife.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "linepay")
@Getter
@Setter
public class LinePayConfig {
    private String channelId;
    private String channelSecret;
    private String apiUrl;
    private String confirmUrl;
}
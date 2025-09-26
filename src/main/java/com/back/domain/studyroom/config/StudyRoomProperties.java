package com.back.domain.studyroom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "studyroom")
public class StudyRoomProperties {
    
    private Heartbeat heartbeat = new Heartbeat();
    private Default defaultSettings = new Default();
    
    @Getter
    @Setter
    public static class Heartbeat {
        private int timeoutMinutes = 5;
    }
    
    @Getter
    @Setter
    public static class Default {
        private int maxParticipants = 10;
        private boolean allowCamera = true;
        private boolean allowAudio = true;
        private boolean allowScreenShare = true;
    }
}

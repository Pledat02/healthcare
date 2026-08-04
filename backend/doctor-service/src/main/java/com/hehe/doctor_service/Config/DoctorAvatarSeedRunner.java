package com.hehe.doctor_service.Config;

import com.hehe.doctor_service.service.DoctorAvatarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "medibook.doctor-avatar.seed-enabled", havingValue = "true")
public class DoctorAvatarSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DoctorAvatarSeedRunner.class);

    private final ResourcePatternResolver resources;
    private final DoctorAvatarService avatarService;

    public DoctorAvatarSeedRunner(ResourcePatternResolver resources, DoctorAvatarService avatarService) {
        this.resources = resources;
        this.avatarService = avatarService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int imported = 0;
        for (Resource resource : resources.getResources("classpath:/avatar-seed/*.jpg")) {
            String filename = resource.getFilename();
            if (filename == null || !filename.endsWith(".jpg")) continue;
            String email = filename.substring(0, filename.length() - 4);
            if (!email.endsWith("@medibook.local")) {
                log.warn("Skipping non-demo avatar seed file: {}", filename);
                continue;
            }
            if (avatarService.seedApproved(email, resource.getContentAsByteArray())) imported++;
        }
        log.info("Doctor avatar seed finished: {} new demo avatars imported", imported);
    }
}

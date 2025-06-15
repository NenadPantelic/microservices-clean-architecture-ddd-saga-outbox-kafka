package com.food.ordering.system.outbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    // a custom object mapper can be created here
    // no need for it in our case - the default one is good for our use case
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper()
//                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//                .configure(DeserializationFeature.FAIL_ON_UNKNOW_PROPERTIES, false)
//                .registerModule(new JavaTimeModule());
//    }

}

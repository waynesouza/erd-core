package com.erd.core.client;

import com.erd.core.dto.request.EmailRequestDTO;
import com.erd.core.dto.response.EmailResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "email-service",
    url = "${emailService.baseUrl:http://localhost:8081/api/v1}"
)
public interface EmailServiceFeignClient {

    @PostMapping("/emails")
    EmailResponseDTO sendEmail(@RequestBody EmailRequestDTO request);

}

package io.github.asmitmans.iotbackend;

import io.github.asmitmans.iotbackend.security.JoinCodeProperties;
import io.github.asmitmans.iotbackend.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, JoinCodeProperties.class})
@EnableSpringDataWebSupport(pageSerializationMode =
		EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class IoTBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IoTBackendApplication.class, args);
	}

}

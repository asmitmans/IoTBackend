package io.github.asmitmans.iotbackend;

import io.github.asmitmans.iotbackend.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class IoTBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IoTBackendApplication.class, args);
	}

}

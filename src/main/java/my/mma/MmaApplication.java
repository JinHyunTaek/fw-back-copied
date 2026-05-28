package my.mma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MmaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MmaApplication.class, args);
	}

}

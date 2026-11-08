// File: src/main/java/az/arena/icearena/IceArenaApplication.java
package az.arena.icearena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IceArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IceArenaApplication.class, args);
    }
}

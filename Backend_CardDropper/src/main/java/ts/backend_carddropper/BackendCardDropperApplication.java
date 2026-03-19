package ts.backend_carddropper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class BackendCardDropperApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendCardDropperApplication.class, args);
    }

}

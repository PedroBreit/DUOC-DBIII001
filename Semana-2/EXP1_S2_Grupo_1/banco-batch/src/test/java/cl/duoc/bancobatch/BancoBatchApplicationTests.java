package cl.duoc.bancobatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false"
})
class BancoBatchApplicationTests {

    @Test
    void contextLoads() {
    }
}

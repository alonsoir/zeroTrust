package com.example.zerotrust;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ZeroTrustApplicationTests {

    @Test
    void contextLoads() {
        // Test que la aplicación cargue correctamente
    }

    @Test
    void mainMethodRunsWithoutError() {
        // Test que el método main no lance excepciones
        ZeroTrustApplication.main(new String[]{});
    }
}

package it.dgc.verificac19;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VerifierServiceTest {

  @Test
  public void contextLoads() {
    // TODO Write test
  }

  @SpringBootApplication(scanBasePackages = "it.dgc")
  static class TestConfiguration {

  }

}

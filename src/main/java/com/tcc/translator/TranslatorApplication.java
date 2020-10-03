package com.tcc.translator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class TranslatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(TranslatorApplication.class, args);
  }
}

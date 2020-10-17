package com.tcc.translator.service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.google.common.base.Splitter;
import com.tcc.translator.dto.GitHubContentForTranslation;
import com.tcc.translator.dto.TranslatedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TranslationService {

  private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

  @Autowired
  private AmazonTranslate translateClient;

  public Mono<TranslatedFile> translateFile(Mono<GitHubContentForTranslation> file) {
    logger.info("Translating file with AWS Translate");

    return file.flatMap(this::translateWithAws);
  }

  private Mono<TranslatedFile> translateWithAws(GitHubContentForTranslation file) {
    Flux<String> chunks = splitContentIntoChunks(file.getContent());

    Mono<String> translationResult = chunks.map(chunk -> {
      TranslateTextRequest request = new TranslateTextRequest()
        .withText(chunk)
        .withSourceLanguageCode(file.getSourceLanguage())
        .withTargetLanguageCode(file.getTargetLanguage());
      return request;
    })
    .flatMap(request -> Mono.fromCallable(() -> translateClient.translateText(request)))
    .map(translation -> translation.getTranslatedText())
    .collect(Collectors.joining());

    Mono<TranslatedFile> translatedFileMono = Mono.just(new TranslatedFile());

    // naoo da pra dar subscribe aqui eu acho, e talvez isso nao funcione como no extractor tb
    translatedFileMono.subscribe(translatedFile -> {
      translationResult.subscribe(result -> {
        translatedFile.setFileName(generateFileName(file.getName(), file.getTargetLanguage()));
        translatedFile.setContent(result);
      });
    });

    return translatedFileMono;
  }

  private Flux<String> splitContentIntoChunks(String fileContent) {
    return Flux.fromIterable(Splitter.fixedLength(4859).split(fileContent));
  }

  private String generateFileName(String fileName, String targetLanguage) {
    StringBuilder sb = new StringBuilder();
    return sb.append(LocalDateTime.now().hashCode())
      .append("_")
      .append(targetLanguage)
      .append("_")
      .append(fileName)
      .toString();
  }
}

package it.albertotn.ollamatest;

import java.io.File;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class Application {

	@Value("${app.vectorstore.path}")
	private String vectorStorePath;

	@Value("${app.resource}")
	private Resource pdfResource;

	@Value("${app.model.name}")
	private String model;

	@Bean
	ChatClient chatClient(ChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

	@SuppressWarnings("deprecation")
	@Bean
	SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
		log.info("Init started");
		OllamaEmbeddingModel ollamaEmbedding = (OllamaEmbeddingModel) embeddingModel;
		ollamaEmbedding.withModel(model);
		SimpleVectorStore simpleVectorStore = new SimpleVectorStore(ollamaEmbedding);
		File vectorStoreFile = new File(vectorStorePath);
		if (vectorStoreFile.exists()) { // load existing vector store if exists
			simpleVectorStore.load(vectorStoreFile);
		} else { // otherwise load the documents and save the vector store
			TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
			List<Document> documents = documentReader.get();
			TextSplitter textSplitter = new TokenTextSplitter();
			List<Document> splitDocuments = textSplitter.apply(documents);
			simpleVectorStore.add(splitDocuments);
			simpleVectorStore.save(vectorStoreFile);
		}
		log.info("Init completed");
		return simpleVectorStore;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
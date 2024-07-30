package it.albertotn.spring_ollama_example.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.albertotn.ollamatest.Answer;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ChatController {

	private final ChatClient aiClient;

	private final VectorStore vectorStore;

	@Value("classpath:/rag-prompt-template.st")
	private Resource ragPromptTemplate;
	
	@Value("classpath:/simple-prompt-template.st")
	private Resource codePromptTemplate;

	@Value("${app.model.name}")
	private String model;

	@Autowired
	public ChatController(ChatClient aiClient, VectorStore vectoreStore) {
		this.aiClient = aiClient;
		this.vectorStore = vectoreStore;
	}
	
	@GetMapping("/ai/ollama/chat")
	public Answer ask(@RequestParam String message) {
		long time = System.currentTimeMillis();
		log.info(String.format("Calling the model with message %s", message));
		String answer = aiClient.prompt()
				.user(userSpec -> userSpec.text(message))
				.options(OllamaOptions.create().withModel(model).withTemperature(0f)).call().content();

		log.info(String.format("Response from model, time %d seconds", (System.currentTimeMillis() - time) / 1000));
		return new Answer(answer);
	}
	
	@GetMapping("/ai/ollama/chat/code")
	public Answer code(@RequestParam String message) {
		long time = System.currentTimeMillis();
		log.info(String.format("Calling the model with message %s", message));
		String answer = aiClient.prompt()
				.user(userSpec -> userSpec.text(codePromptTemplate).param("input", message))
				.options(OllamaOptions.create().withModel(model).withTemperature(0f)).call().content();
		
		log.info(String.format("Response from model, time %d seconds", (System.currentTimeMillis() - time) / 1000));
		return new Answer(answer);
	}

	@GetMapping("/ai/ollama/chat/rag")
	public Answer rag(@RequestParam String message) {
		long time = System.currentTimeMillis();
		log.info(String.format("Calling the model with message %s", message));
		List<Document> similarDocuments = vectorStore
				.similaritySearch(SearchRequest.query(message).withTopK(2));
		List<String> contentList = similarDocuments.stream().map(Document::getContent).toList();
		String answer = aiClient.prompt()
				.user(userSpec -> userSpec.text(ragPromptTemplate).param("input", message)
						.param("documents", String.join("\n", contentList)))
				.options(OllamaOptions.create().withModel(model).withTemperature(0f)).call().content();

		log.info(String.format("Response from model, time %d seconds", (System.currentTimeMillis() - time) / 1000));
		return new Answer(answer);
	}

}

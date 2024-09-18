package ua.spro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as
 * possible Don't worry about performance, concurrency, etc You can use in Memory collection for
 * sore data
 *
 * <p>Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class This class could be auto tested
 */
public class DocumentManager {

  private final List<Document> documents = new ArrayList<>();

  /**
   * Implementation of this method should upsert the document to your storage And generate unique id
   * if it does not exist, don't change [created] field
   *
   * @param document - document content and author data
   * @return saved document
   */
  public Document save(Document document) {
    if (document == null) {
      throw new IllegalArgumentException("Document cannot be null");
    }

    // If the document doesn't have an ID, generate one
    if (document.getId() == null) {
      document.setId(UUID.randomUUID().toString());
    }

    // Replace the document if it already exists
    documents.removeIf(d -> d.getId().equals(document.getId()));
    documents.add(document);

    return document;
  }


  /**
   * Implementation this method should find documents which match with request
   *
   * @param request - search request, each field could be null
   * @return list matched documents
   */
  public List<Document> search(SearchRequest request) {
    if (request == null) {
      return Collections.emptyList();
    }

    return documents.stream()
        .filter(document -> matchesTitlePrefixes(document, request.titlePrefixes))
        .filter(document -> matchesContent(document, request.containsContents))
        .filter(document -> matchesAuthorIds(document, request.authorIds))
        .filter(document -> matchesCreatedFrom(document, request.createdFrom))
        .filter(document -> matchesCreatedTo(document, request.createdTo))
        .toList();
  }

  /**
   * Implementation this method should find document by id
   *
   * @param id - document id
   * @return optional document
   */
  public Optional<Document> findById(String id) {
    return documents.stream().filter(d -> d.getId().equals(id)).findFirst();
  }

  private boolean matchesTitlePrefixes(Document document, List<String> titlePrefixes) {
    if (titlePrefixes == null || titlePrefixes.isEmpty()) {
      return true;
    }
    return titlePrefixes.stream().anyMatch(prefix -> document.getTitle().startsWith(prefix));
  }

  private boolean matchesContent(Document document, List<String> containsContents) {
    if (containsContents == null || containsContents.isEmpty()) {
      return true;
    }
    return containsContents.stream().anyMatch(content -> document.getContent().contains(content));
  }

  private boolean matchesAuthorIds(Document document, List<String> authorIds) {
    if (authorIds == null || authorIds.isEmpty()) {
      return true;
    }
    return authorIds.contains(document.getAuthor().getId());
  }

  private boolean matchesCreatedFrom(Document document, Instant createdFrom) {
    if (createdFrom == null) {
      return true;
    }
    return document.getCreated().isAfter(createdFrom);
  }

  private boolean matchesCreatedTo(Document document, Instant createdTo) {
    if (createdTo == null) {
      return true;
    }
    return document.getCreated().isBefore(createdTo);
  }



  @Data
  @Builder
  public static class SearchRequest {
    private List<String> titlePrefixes;
    private List<String> containsContents;
    private List<String> authorIds;
    private Instant createdFrom;
    private Instant createdTo;
  }

  @Data
  @Builder
  public static class Document {
    private String id;
    private String title;
    private String content;
    private Author author;
    private Instant created;
  }

  @Data
  @Builder
  public static class Author {
    private String id;
    private String name;
  }
}

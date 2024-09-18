package ua.spro;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ua.spro.DocumentManager.Author;
import ua.spro.DocumentManager.Document;
import ua.spro.DocumentManager.SearchRequest;

class DocumentManagerTest {

  private DocumentManager documentManager;

  @BeforeEach
  void setUp() {
    documentManager = new DocumentManager();
  }

  @Nested
  class Save {

    @Test
    void save_shouldGenerateId_whenIdIsNull() {
      // Given
      Document document =
          Document.builder()
              .title("Test Document")
              .content("Sample content")
              .author(Author.builder().id("author1").name("Author One").build())
              .created(Instant.now())
              .build();

      // When
      Document savedDocument = documentManager.save(document);

      // Then
      assertNotNull(savedDocument.getId());
      assertEquals("Test Document", savedDocument.getTitle());
      assertEquals("Sample content", savedDocument.getContent());
    }

    @Test
    void save_shouldReplaceDocument_whenIdExists() {
      // Given
      String existingId = UUID.randomUUID().toString();
      Document oldDocument =
          Document.builder()
              .id(existingId)
              .title("Old Document")
              .content("Old content")
              .author(Author.builder().id("author1").name("Author One").build())
              .created(Instant.now())
              .build();

      // Save old document first
      documentManager.save(oldDocument);

      // New document with same ID
      Document newDocument =
          Document.builder()
              .id(existingId)
              .title("New Document")
              .content("New content")
              .author(Author.builder().id("author1").name("Author One").build())
              .created(Instant.now())
              .build();

      // When
      Document savedDocument = documentManager.save(newDocument);

      // Then
      assertEquals(existingId, savedDocument.getId());
      assertEquals("New Document", savedDocument.getTitle());
      assertEquals("New content", savedDocument.getContent());
    }

    @Test
    void save_shouldThrowException_whenDocumentIsNull() {
      // Given
      Document nullDocument = null;

      // When & Then
      assertThrows(IllegalArgumentException.class, () -> documentManager.save(nullDocument));
    }

    @Test
    void save_shouldNotChangeCreatedTimestamp_whenUpdatingDocument() {
      // Given
      Instant createdTimestamp = Instant.now();
      Document document =
          Document.builder()
              .title("Test Document")
              .content("Sample content")
              .author(Author.builder().id("author1").name("Author One").build())
              .created(createdTimestamp)
              .build();

      // Save the document
      Document savedDocument = documentManager.save(document);

      // When: Update the document
      savedDocument.setTitle("Updated Title");
      documentManager.save(savedDocument);

      // Then
      assertEquals(createdTimestamp, savedDocument.getCreated());
      assertEquals("Updated Title", savedDocument.getTitle());
    }
  }

  @Nested
  class FindById {
    @Test
    void findById_shouldReturnDocument_whenIdExists() {
      // Given
      String documentId = UUID.randomUUID().toString();
      Document document =
          Document.builder()
              .id(documentId)
              .title("Test Document")
              .content("Sample content")
              .author(Author.builder().id("author1").name("Author One").build())
              .created(Instant.now())
              .build();
      documentManager.save(document);

      // When
      Optional<Document> foundDocument = documentManager.findById(documentId);

      // Then
      assertTrue(foundDocument.isPresent());
      assertEquals(documentId, foundDocument.get().getId());
      assertEquals("Test Document", foundDocument.get().getTitle());
    }

    @Test
    void findById_shouldReturnEmptyOptional_whenIdDoesNotExist() {
      String nonExistingId = UUID.randomUUID().toString();

      Optional<Document> foundDocument = documentManager.findById(nonExistingId);

      assertFalse(foundDocument.isPresent());
    }

    @Test
    void findById_shouldReturnEmptyOptional_whenIdIsNull() {
      Optional<Document> foundDocument = documentManager.findById(null);

      assertFalse(foundDocument.isPresent());
    }
  }

  @Nested
  class Search {
    private DocumentManager documentManager;

    @BeforeEach
    void setUp() {
      documentManager = new DocumentManager();

      // Sample documents
      // 1 hour ago
      Document document1 =
          Document.builder()
              .id(UUID.randomUUID().toString())
              .title("Java Programming")
              .content("Learn Java step by step")
              .author(Author.builder().id("author1").name("Alice").build())
              .created(Instant.now().minusSeconds(3600)) // 1 hour ago
              .build();

      // 2 hours ago
      Document document2 =
          Document.builder()
              .id(UUID.randomUUID().toString())
              .title("Python Programming")
              .content("Master Python easily")
              .author(Author.builder().id("author2").name("Bob").build())
              .created(Instant.now().minusSeconds(7200)) // 2 hours ago
              .build();

      // 3 hours ago
      Document document3 =
          Document.builder()
              .id(UUID.randomUUID().toString())
              .title("Java and Spring Boot")
              .content("Learn Java and Spring Boot for web development")
              .author(Author.builder().id("author1").name("Alice").build())
              .created(Instant.now().minusSeconds(10800)) // 3 hours ago
              .build();

      // Save documents to manager
      documentManager.save(document1);
      documentManager.save(document2);
      documentManager.save(document3);
    }

    @ParameterizedTest
    @MethodSource("provideSearchRequests")
    void search_shouldReturnMatchingDocuments(SearchRequest searchRequest, int expectedSize) {
      List<Document> foundDocuments = documentManager.search(searchRequest);

      assertEquals(expectedSize, foundDocuments.size());
    }

    static Stream<Object[]> provideSearchRequests() {
      return Stream.of(
          // Search by title prefix "Java"
          new Object[] {SearchRequest.builder().titlePrefixes(List.of("Java")).build(), 2},
          // Search by content "Python"
          new Object[] {SearchRequest.builder().containsContents(List.of("Python")).build(), 1},
          // Search by author "author1"
          new Object[] {SearchRequest.builder().authorIds(List.of("author1")).build(), 2},
          // Search by createdFrom and createdTo range
          new Object[] {
            SearchRequest.builder()
                .createdFrom(Instant.now().minusSeconds(7200)) // 2 hours ago
                .createdTo(Instant.now())
                .build(),
            2
          },
          // Search by multiple criteria: author "author1" and title prefix "Java"
          new Object[] {
            SearchRequest.builder()
                .authorIds(List.of("author1"))
                .titlePrefixes(List.of("Java"))
                .build(),
            2
          },
          // Search by multiple criteria: content "Python" and createdFrom
          new Object[] {
            SearchRequest.builder()
                .containsContents(List.of("Python"))
                .createdFrom(Instant.now().minusSeconds(10800)) // 3 hours ago
                .build(),
            1
          },
          // Empty search request (no filters)
          new Object[] {SearchRequest.builder().build(), 3},
          // Search by non-matching criteria
          new Object[] {
            SearchRequest.builder().titlePrefixes(List.of("NonExistingPrefix")).build(), 0
          });
    }
  }
}

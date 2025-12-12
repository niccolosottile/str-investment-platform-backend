package com.str.platform.location.infrastructure.persistence.adapter;

import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import com.str.platform.location.infrastructure.persistence.mapper.LocationEntityMapper;
import com.str.platform.location.infrastructure.persistence.repository.JpaLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for JpaLocationRepositoryAdapter using TestContainers.
 * This spins up a real PostgreSQL database in Docker for testing.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaLocationRepositoryAdapter.class, LocationEntityMapper.class})
class JpaLocationRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private JpaLocationRepositoryAdapter repository;

    @Autowired
    private JpaLocationRepository jpaRepository;

    private Location testLocation;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();

        Coordinates coordinates = new Coordinates(41.9028, 12.4964); // Rome
        Address address = new Address("Rome", "Lazio", "Italy");
        testLocation = new Location(null, coordinates, address);
    }

    @Test
    void shouldSaveAndRetrieveLocation() {
        // When
        Location saved = repository.save(testLocation);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCoordinates().latitude()).isEqualTo(41.9028);
        assertThat(saved.getAddress().city()).isEqualTo("Rome");
    }

    @Test
    void shouldFindLocationById() {
        // Given
        Location saved = repository.save(testLocation);

        // When
        Optional<Location> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAddress().city()).isEqualTo("Rome");
    }

    @Test
    void shouldFindLocationByCoordinates() {
        // Given
        repository.save(testLocation);

        // When
        Optional<Location> found = repository.findByCoordinates(41.9028, 12.4964);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAddress().city()).isEqualTo("Rome");
    }

    @Test
    void shouldFindNearbyLocations() {
        // Given
        repository.save(testLocation);

        Coordinates milanCoords = new Coordinates(45.4642, 9.1900);
        Address milanAddress = new Address("Milan", "Lombardy", "Italy");
        Location milan = new Location(null, milanCoords, milanAddress);
        repository.save(milan);

        // When - Search within 600km of Rome (should find both)
        List<Location> nearby = repository.findNearby(41.9028, 12.4964, 600);

        // Then
        assertThat(nearby).hasSize(2);
    }

    @Test
    void shouldSearchLocationsByQuery() {
        // Given
        repository.save(testLocation);

        Coordinates parisCoords = new Coordinates(48.8566, 2.3522);
        Address parisAddress = new Address("Paris", "Île-de-France", "France");
        Location paris = new Location(null, parisCoords, parisAddress);
        repository.save(paris);

        // When
        List<Location> results = repository.searchByQuery("rome");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAddress().city()).isEqualTo("Rome");
    }

    @Test
    void shouldUpdateExistingLocation() {
        // Given
        Location saved = repository.save(testLocation);
        saved.updateScrapedData(150, 85.50);

        // When
        Location updated = repository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getPropertyCount()).isEqualTo(150);
        assertThat(updated.getAveragePrice()).isEqualTo(85.50);
    }

    @Test
    void shouldDeleteLocation() {
        // Given
        Location saved = repository.save(testLocation);
        UUID id = saved.getId();

        // When
        repository.delete(id);

        // Then
        Optional<Location> found = repository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenLocationNotFound() {
        // When
        Optional<Location> found = repository.findById(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }
}

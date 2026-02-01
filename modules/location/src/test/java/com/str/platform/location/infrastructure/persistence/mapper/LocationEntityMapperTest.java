package com.str.platform.location.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocationEntityMapper")
class LocationEntityMapperTest {

    private static final double LATITUDE = 45.4642;
    private static final double LONGITUDE = 9.1900;
    private static final String CITY = "Milan";
    private static final String REGION = "Lombardy";
    private static final String COUNTRY = "Italy";
    private static final String FULL_ADDRESS = "Milan, Lombardy, Italy";
    private static final double SW_LNG = 9.0;
    private static final double SW_LAT = 45.3;
    private static final double NE_LNG = 9.3;
    private static final double NE_LAT = 45.6;

    private LocationEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LocationEntityMapper();
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    class EntityToDomainMapping {

        @Test
        void shouldMapBasicEntityToDomain() {
            var entity = createBasicLocationEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
            assertThat(domain.getCoordinates().getLatitude()).isEqualTo(LATITUDE);
            assertThat(domain.getCoordinates().getLongitude()).isEqualTo(LONGITUDE);
            assertThat(domain.getAddress().getCity()).isEqualTo(CITY);
            assertThat(domain.getAddress().getRegion()).isEqualTo(REGION);
            assertThat(domain.getAddress().getCountry()).isEqualTo(COUNTRY);
            assertThat(domain.getAddress().getFullAddress()).isEqualTo(FULL_ADDRESS);
        }

        @Test
        void shouldMapEntityWithBoundingBoxToDomain() {
            var entity = createLocationEntityWithBoundingBox();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getBoundingBox()).isNotNull();
            assertThat(domain.getBoundingBox().getSouthWestLongitude()).isEqualTo(SW_LNG);
            assertThat(domain.getBoundingBox().getSouthWestLatitude()).isEqualTo(SW_LAT);
            assertThat(domain.getBoundingBox().getNorthEastLongitude()).isEqualTo(NE_LNG);
            assertThat(domain.getBoundingBox().getNorthEastLatitude()).isEqualTo(NE_LAT);
        }

        @Test
        void shouldHandleNullBoundingBoxCoordinates() {
            var entity = createBasicLocationEntity();
            entity.setBoundingBoxSwLng(null);
            entity.setBoundingBoxSwLat(null);
            entity.setBoundingBoxNeLng(null);
            entity.setBoundingBoxNeLat(null);

            var domain = mapper.toDomain(entity);

            assertThat(domain.getBoundingBox()).isNull();
        }

        @Test
        void shouldMapScrapingMetadata() {
            var entity = createBasicLocationEntity();
            var lastScraped = Instant.now().minusSeconds(3600);
            entity.setLastScraped(lastScraped);
            entity.setPropertyCount(42);

            var domain = mapper.toDomain(entity);

            assertThat(domain.getLastScraped()).isNotNull();
            assertThat(domain.getPropertyCount()).isEqualTo(42);
        }

        @Test
        void shouldReturnNullForNullEntity() {
            var domain = mapper.toDomain(null);

            assertThat(domain).isNull();
        }
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    class DomainToEntityMapping {

        @Test
        void shouldMapBasicDomainToEntity() {
            var domain = createBasicLocation();

            var entity = mapper.toEntity(domain);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(domain.getId());
            assertThat(entity.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(LATITUDE));
            assertThat(entity.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(LONGITUDE));
            assertThat(entity.getCity()).isEqualTo(CITY);
            assertThat(entity.getRegion()).isEqualTo(REGION);
            assertThat(entity.getCountry()).isEqualTo(COUNTRY);
            assertThat(entity.getFullAddress()).isEqualTo(FULL_ADDRESS);
        }

        @Test
        void shouldMapDomainWithBoundingBoxToEntity() {
            var domain = createLocationWithBoundingBox();

            var entity = mapper.toEntity(domain);

            assertThat(entity.getBoundingBoxSwLng()).isEqualByComparingTo(BigDecimal.valueOf(SW_LNG));
            assertThat(entity.getBoundingBoxSwLat()).isEqualByComparingTo(BigDecimal.valueOf(SW_LAT));
            assertThat(entity.getBoundingBoxNeLng()).isEqualByComparingTo(BigDecimal.valueOf(NE_LNG));
            assertThat(entity.getBoundingBoxNeLat()).isEqualByComparingTo(BigDecimal.valueOf(NE_LAT));
        }

        @Test
        void shouldMapDataQualityEnum() {
            var domain = createBasicLocation();
            domain.updateScrapingData(100, LocalDateTime.now());

            var entity = mapper.toEntity(domain);

            assertThat(entity.getDataQuality()).isEqualTo(LocationEntity.DataQuality.HIGH);
        }

        @Test
        void shouldReturnNullForNullDomain() {
            var entity = mapper.toEntity(null);

            assertThat(entity).isNull();
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping Consistency")
    class BidirectionalMappingConsistency {

        @Test
        void shouldMaintainDataIntegrityThroughRoundTrip() {
            var originalEntity = createLocationEntityWithBoundingBox();
            originalEntity.setLastScraped(Instant.now());
            originalEntity.setPropertyCount(50);

            var domain = mapper.toDomain(originalEntity);
            var mappedEntity = mapper.toEntity(domain);

            assertThat(mappedEntity.getLatitude()).isEqualByComparingTo(originalEntity.getLatitude());
            assertThat(mappedEntity.getLongitude()).isEqualByComparingTo(originalEntity.getLongitude());
            assertThat(mappedEntity.getCity()).isEqualTo(originalEntity.getCity());
            assertThat(mappedEntity.getBoundingBoxSwLng()).isEqualByComparingTo(originalEntity.getBoundingBoxSwLng());
            assertThat(mappedEntity.getBoundingBoxNeLat()).isEqualByComparingTo(originalEntity.getBoundingBoxNeLat());
        }
    }

    private LocationEntity createBasicLocationEntity() {
        return LocationEntity.builder()
            .id(UUID.randomUUID())
            .latitude(BigDecimal.valueOf(LATITUDE))
            .longitude(BigDecimal.valueOf(LONGITUDE))
            .city(CITY)
            .region(REGION)
            .country(COUNTRY)
            .fullAddress(FULL_ADDRESS)
            .dataQuality(LocationEntity.DataQuality.MEDIUM)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private LocationEntity createLocationEntityWithBoundingBox() {
        var entity = createBasicLocationEntity();
        entity.setBoundingBoxSwLng(BigDecimal.valueOf(SW_LNG));
        entity.setBoundingBoxSwLat(BigDecimal.valueOf(SW_LAT));
        entity.setBoundingBoxNeLng(BigDecimal.valueOf(NE_LNG));
        entity.setBoundingBoxNeLat(BigDecimal.valueOf(NE_LAT));
        return entity;
    }

    private Location createBasicLocation() {
        var coordinates = new Coordinates(LATITUDE, LONGITUDE);
        var address = new Address(CITY, REGION, COUNTRY, FULL_ADDRESS);
        return new Location(coordinates, address);
    }

    private Location createLocationWithBoundingBox() {
        var coordinates = new Coordinates(LATITUDE, LONGITUDE);
        var address = new Address(CITY, REGION, COUNTRY, FULL_ADDRESS);
        var bbox = new BoundingBox(SW_LNG, SW_LAT, NE_LNG, NE_LAT);
        return new Location(coordinates, address, bbox);
    }
}

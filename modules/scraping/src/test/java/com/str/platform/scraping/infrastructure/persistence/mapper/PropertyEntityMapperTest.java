package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PropertyEntityMapper")
class PropertyEntityMapperTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final String PLATFORM_ID = "airbnb-123456";
    private static final double LATITUDE = 45.4642;
    private static final double LONGITUDE = 9.1900;
    private static final String TITLE = "Elegant Apartment in Milan Center";
    private static final String PROPERTY_URL = "https://airbnb.com/rooms/123456";
    private static final String IMAGE_URL = "https://images.airbnb.com/123456.jpg";

    private PropertyEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PropertyEntityMapper();
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    class EntityToDomainMapping {

        @Test
        void shouldMapBasicPropertyEntityToDomain() {
            var entity = createBasicPropertyEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
            assertThat(domain.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(domain.getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);
            assertThat(domain.getPlatformId()).isEqualTo(PLATFORM_ID);
            assertThat(domain.getCoordinates().getLatitude()).isEqualTo(LATITUDE);
            assertThat(domain.getCoordinates().getLongitude()).isEqualTo(LONGITUDE);
        }

        @Test
        void shouldMapPropertyDetails() {
            var entity = createPropertyEntityWithDetails();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getBedrooms()).isEqualTo(2);
            assertThat(domain.getBathrooms()).isEqualTo(1.0);
            assertThat(domain.getMaxGuests()).isEqualTo(4);
        }

        @Test
        void shouldMapRatingAndReviews() {
            var entity = createBasicPropertyEntity();
            entity.setRating(BigDecimal.valueOf(4.85));
            entity.setReviewCount(127);

            var domain = mapper.toDomain(entity);

            assertThat(domain.getRating()).isEqualTo(4.85);
            assertThat(domain.getReviewCount()).isEqualTo(127);
        }

        @Test
        void shouldHandleNullRating() {
            var entity = createBasicPropertyEntity();
            entity.setRating(null);
            entity.setReviewCount(null);

            var domain = mapper.toDomain(entity);

            assertThat(domain.getRating()).isZero();
            assertThat(domain.getReviewCount()).isZero();
        }

        @Test
        void shouldMapMetadata() {
            var entity = createPropertyEntityWithMetadata();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getTitle()).isEqualTo(TITLE);
            assertThat(domain.getPropertyUrl()).isEqualTo(PROPERTY_URL);
            assertThat(domain.getImageUrl()).isEqualTo(IMAGE_URL);
            assertThat(domain.getIsSuperhost()).isTrue();
        }

        @Test
        void shouldMapAllPropertyTypes() {
            var entity = createBasicPropertyEntity();
            
            entity.setPropertyType("ENTIRE_APARTMENT");
            assertThat(mapper.toDomain(entity).getPropertyType()).isEqualTo(Property.PropertyType.ENTIRE_APARTMENT);
            
            entity.setPropertyType("ENTIRE_HOUSE");
            assertThat(mapper.toDomain(entity).getPropertyType()).isEqualTo(Property.PropertyType.ENTIRE_HOUSE);
            
            entity.setPropertyType("PRIVATE_ROOM");
            assertThat(mapper.toDomain(entity).getPropertyType()).isEqualTo(Property.PropertyType.PRIVATE_ROOM);
            
            entity.setPropertyType("SHARED_ROOM");
            assertThat(mapper.toDomain(entity).getPropertyType()).isEqualTo(Property.PropertyType.SHARED_ROOM);
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
        void shouldMapBasicPropertyToEntity() {
            var domain = createBasicProperty();

            var entity = mapper.toEntity(domain);

            assertThat(entity).isNotNull();
            assertThat(entity.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(entity.getPlatform()).isEqualTo(PropertyEntity.Platform.AIRBNB);
            assertThat(entity.getPlatformPropertyId()).isEqualTo(PLATFORM_ID);
            assertThat(entity.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(LATITUDE));
            assertThat(entity.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(LONGITUDE));
            assertThat(entity.getPropertyType()).isEqualTo("ENTIRE_APARTMENT");
        }

        @Test
        void shouldMapPropertyDetailsToEntity() {
            var domain = createPropertyWithDetails();

            var entity = mapper.toEntity(domain);

            assertThat(entity.getBedrooms()).isEqualTo(3);
            assertThat(entity.getBathrooms()).isEqualTo(2);
            assertThat(entity.getGuests()).isEqualTo(6);
        }

        @Test
        void shouldMapRatingToEntity() {
            var domain = createBasicProperty();
            domain.setRating(4.92, 215);

            var entity = mapper.toEntity(domain);

            assertThat(entity.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.92));
            assertThat(entity.getReviewCount()).isEqualTo(215);
        }

        @Test
        void shouldHandleZeroRating() {
            var domain = createBasicProperty();

            var entity = mapper.toEntity(domain);

            assertThat(entity.getRating()).isNull();
        }

        @Test
        void shouldReturnNullForNullDomain() {
            var entity = mapper.toEntity(null);

            assertThat(entity).isNull();
        }
    }

    @Nested
    @DisplayName("Platform Enum Mapping")
    class PlatformEnumMapping {

        @Test
        void shouldMapAllPlatformsCorrectly() {
            var airbnbEntity = createBasicPropertyEntity();
            airbnbEntity.setPlatform(PropertyEntity.Platform.AIRBNB);
            assertThat(mapper.toDomain(airbnbEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);

            var bookingEntity = createBasicPropertyEntity();
            bookingEntity.setPlatform(PropertyEntity.Platform.BOOKING);
            assertThat(mapper.toDomain(bookingEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.BOOKING);

            var vrboEntity = createBasicPropertyEntity();
            vrboEntity.setPlatform(PropertyEntity.Platform.VRBO);
            assertThat(mapper.toDomain(vrboEntity).getPlatform()).isEqualTo(ScrapingJob.Platform.VRBO);
        }
    }

    private PropertyEntity createBasicPropertyEntity() {
        return PropertyEntity.builder()
            .id(UUID.randomUUID())
            .locationId(LOCATION_ID)
            .platform(PropertyEntity.Platform.AIRBNB)
            .platformPropertyId(PLATFORM_ID)
            .latitude(BigDecimal.valueOf(LATITUDE))
            .longitude(BigDecimal.valueOf(LONGITUDE))
            .propertyType("ENTIRE_APARTMENT")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private PropertyEntity createPropertyEntityWithDetails() {
        var entity = createBasicPropertyEntity();
        entity.setBedrooms(2);
        entity.setBathrooms(1);
        entity.setGuests(4);
        return entity;
    }

    private PropertyEntity createPropertyEntityWithMetadata() {
        var entity = createBasicPropertyEntity();
        entity.setTitle(TITLE);
        entity.setPropertyUrl(PROPERTY_URL);
        entity.setImageUrl(IMAGE_URL);
        entity.setIsSuperhost(true);
        return entity;
    }

    private Property createBasicProperty() {
        var coordinates = new Coordinates(LATITUDE, LONGITUDE);
        return new Property(
            LOCATION_ID,
            ScrapingJob.Platform.AIRBNB,
            PLATFORM_ID,
            coordinates,
            Property.PropertyType.ENTIRE_APARTMENT
        );
    }

    private Property createPropertyWithDetails() {
        var property = createBasicProperty();
        property.setDetails(3, 2.0, 6);
        return property;
    }
}

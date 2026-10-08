package com.travel.travel_service.config;

import com.travel.travel_service.model.ItineraryEntity;
import com.travel.travel_service.model.TravelEntity;
import com.travel.travel_service.repository.TravelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

        private final TravelRepository travelRepository;

        public DataInitializer(TravelRepository travelRepository) {
                this.travelRepository = travelRepository;
        }

        @Override
        public void run(String... args) {

                if (travelRepository.count() > 0) {
                        log.info("Travel database already contains data. Skipping initialization.");
                        return;
                }

                List<TravelEntity> travels = List.of(

                                TravelEntity.builder()
                                                .travelId(1L)
                                                .title("Discover Paris")
                                                .city("Paris")
                                                .country("France")
                                                .description(
                                                                "A three-day journey through Paris, combining iconic landmarks, "
                                                                                + "French culture, museums and local experiences.")
                                                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/1/15/Paris_from_the_Arc_de_Triomphe%2C_17_October_2019.jpg?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=original")
                                                .duration(3)
                                                .itinerary(List.of(
                                                                ItineraryEntity.builder()
                                                                                .dayNumber(1)
                                                                                .title("Paris Classics")
                                                                                .description("Explore the most iconic landmarks of Paris.")
                                                                                .activities(List.of(
                                                                                                "Eiffel Tower",
                                                                                                "Seine River walk",
                                                                                                "Arc de Triomphe",
                                                                                                "Champs-Élysées"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(2)
                                                                                .title("Art & History")
                                                                                .description("Discover Paris through its museums and historic districts.")
                                                                                .activities(List.of(
                                                                                                "Louvre Museum",
                                                                                                "Tuileries Garden",
                                                                                                "Notre-Dame",
                                                                                                "Latin Quarter"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(3)
                                                                                .title("Montmartre")
                                                                                .description("Experience the artistic side of Paris.")
                                                                                .activities(List.of(
                                                                                                "Sacré-Cœur",
                                                                                                "Montmartre",
                                                                                                "Place du Tertre",
                                                                                                "Local French dinner"))
                                                                                .build()))
                                                .build(),

                                TravelEntity.builder()
                                                .travelId(2L)
                                                .title("Tokyo Experience")
                                                .city("Tokyo")
                                                .country("Japan")
                                                .description(
                                                                "A four-day Tokyo experience mixing traditional Japanese culture, "
                                                                                + "modern technology, food and famous neighborhoods.")
                                                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/archive/6/6d/20251228182740%21Tokyo_Tower%2C_Minato_City.jpg")
                                                .duration(4)
                                                .itinerary(List.of(
                                                                ItineraryEntity.builder()
                                                                                .dayNumber(1)
                                                                                .title("Modern Tokyo")
                                                                                .description("Discover Tokyo's modern city center.")
                                                                                .activities(List.of(
                                                                                                "Shibuya Crossing",
                                                                                                "Shibuya Sky",
                                                                                                "Harajuku",
                                                                                                "Meiji Shrine"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(2)
                                                                                .title("Traditional Tokyo")
                                                                                .description("Explore Tokyo's traditional neighborhoods.")
                                                                                .activities(List.of(
                                                                                                "Senso-ji Temple",
                                                                                                "Asakusa",
                                                                                                "Ueno Park",
                                                                                                "Tokyo National Museum"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(3)
                                                                                .title("Technology & Entertainment")
                                                                                .description("Experience Tokyo's famous technology district.")
                                                                                .activities(List.of(
                                                                                                "Akihabara",
                                                                                                "Electronics district",
                                                                                                "Arcade experience",
                                                                                                "Tokyo nightlife"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(4)
                                                                                .title("Tokyo Food & Shopping")
                                                                                .description("Finish the trip with food and shopping.")
                                                                                .activities(List.of(
                                                                                                "Tsukiji Outer Market",
                                                                                                "Ginza",
                                                                                                "Japanese cooking experience",
                                                                                                "Tokyo Tower"))
                                                                                .build()))
                                                .build(),

                                TravelEntity.builder()
                                                .travelId(3L)
                                                .title("New York City Adventure")
                                                .city("New York City")
                                                .country("United States")
                                                .description(
                                                                "Explore Manhattan's iconic landmarks, neighborhoods, museums "
                                                                                + "and famous New York experiences.")
                                                .imageUrl("https://thumb.wikimedia.org/wikipedia/commons/thumb/0/01/New_York_City_%28New_York%2C_USA%29%2C_Empire_State_Building_--_2012_--_6448.jpg/1920px-New_York_City_%28New_York%2C_USA%29%2C_Empire_State_Building_--_2012_--_6448.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail")
                                                .duration(4)
                                                .itinerary(List.of(
                                                                ItineraryEntity.builder()
                                                                                .dayNumber(1)
                                                                                .title("Manhattan Classics")
                                                                                .description("Visit New York's most recognizable landmarks.")
                                                                                .activities(List.of(
                                                                                                "Times Square",
                                                                                                "Central Park",
                                                                                                "Rockefeller Center",
                                                                                                "5th Avenue"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(2)
                                                                                .title("Downtown New York")
                                                                                .description("Explore Lower Manhattan and its history.")
                                                                                .activities(List.of(
                                                                                                "Statue of Liberty",
                                                                                                "Wall Street",
                                                                                                "9/11 Memorial",
                                                                                                "Brooklyn Bridge"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(3)
                                                                                .title("Arts & Culture")
                                                                                .description("Discover New York's cultural side.")
                                                                                .activities(List.of(
                                                                                                "Metropolitan Museum of Art",
                                                                                                "Museum Mile",
                                                                                                "Broadway",
                                                                                                "Greenwich Village"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(4)
                                                                                .title("Brooklyn")
                                                                                .description("Discover Brooklyn's neighborhoods and views.")
                                                                                .activities(List.of(
                                                                                                "DUMBO",
                                                                                                "Brooklyn Bridge Park",
                                                                                                "Williamsburg",
                                                                                                "Brooklyn food experience"))
                                                                                .build()))
                                                .build(),

                                TravelEntity.builder()
                                                .travelId(4L)
                                                .title("Dubai Discovery")
                                                .city("Dubai")
                                                .country("United Arab Emirates")
                                                .description(
                                                                "Experience Dubai's modern architecture, desert landscapes, "
                                                                                + "shopping districts and cultural attractions.")
                                                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/4/4a/Beach_of_Habtoor_Grand_Resort_Spa%2C_Dubai_UAE.jpg?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=original")
                                                .duration(3)
                                                .itinerary(List.of(
                                                                ItineraryEntity.builder()
                                                                                .dayNumber(1)
                                                                                .title("Modern Dubai")
                                                                                .description("Discover Dubai's famous skyline.")
                                                                                .activities(List.of(
                                                                                                "Burj Khalifa",
                                                                                                "Dubai Mall",
                                                                                                "Dubai Fountain",
                                                                                                "Downtown Dubai"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(2)
                                                                                .title("Old Dubai")
                                                                                .description("Discover the historical side of the city.")
                                                                                .activities(List.of(
                                                                                                "Al Fahidi Historical District",
                                                                                                "Dubai Creek",
                                                                                                "Gold Souk",
                                                                                                "Spice Souk"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(3)
                                                                                .title("Desert Experience")
                                                                                .description("Spend the day outside the city.")
                                                                                .activities(List.of(
                                                                                                "Desert safari",
                                                                                                "Dune experience",
                                                                                                "Traditional dinner",
                                                                                                "Desert sunset"))
                                                                                .build()))
                                                .build(),

                                TravelEntity.builder()
                                                .travelId(5L)
                                                .title("Marrakech Escape")
                                                .city("Marrakech")
                                                .country("Morocco")
                                                .description(
                                                                "Discover Marrakech through its medina, traditional markets, "
                                                                                + "historic architecture, gardens and Moroccan cuisine.")
                                                .imageUrl("https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0c/Coucher_de_soleil_sur_la_place_Jamaa_El_Fna.jpg/1920px-Coucher_de_soleil_sur_la_place_Jamaa_El_Fna.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail")
                                                .duration(3)
                                                .itinerary(List.of(
                                                                ItineraryEntity.builder()
                                                                                .dayNumber(1)
                                                                                .title("The Medina")
                                                                                .description("Explore the historic heart of Marrakech.")
                                                                                .activities(List.of(
                                                                                                "Jemaa el-Fnaa",
                                                                                                "Marrakech Medina",
                                                                                                "Souks",
                                                                                                "Koutoubia Mosque"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(2)
                                                                                .title("Palaces & Gardens")
                                                                                .description("Discover Marrakech's architecture and gardens.")
                                                                                .activities(List.of(
                                                                                                "Bahia Palace",
                                                                                                "El Badi Palace",
                                                                                                "Majorelle Garden",
                                                                                                "Yves Saint Laurent Museum"))
                                                                                .build(),

                                                                ItineraryEntity.builder()
                                                                                .dayNumber(3)
                                                                                .title("Moroccan Experience")
                                                                                .description("Enjoy local food and Moroccan culture.")
                                                                                .activities(List.of(
                                                                                                "Moroccan cooking experience",
                                                                                                "Traditional hammam",
                                                                                                "Local market",
                                                                                                "Moroccan dinner"))
                                                                                .build()))
                                                .build());

                travels.forEach(travel -> {
                                if (travel.getItinerary() != null) {
                                                travel.getItinerary().forEach(itinerary -> itinerary.setTravel(travel));
                                }
                });

                travelRepository.saveAll(travels);

                log.info(
                                "Successfully initialized travel database with {} travel itineraries.",
                                travels.size());
        }
}

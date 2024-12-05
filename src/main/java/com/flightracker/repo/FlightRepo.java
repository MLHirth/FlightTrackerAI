package com.flightracker.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flightracker.domain.Flight;

/**
 * Repository interface for managing Flight entities.
 * Extends JpaRepository to provide CRUD operations and additional query methods.
 */
@Repository
public interface FlightRepo extends JpaRepository<Flight, String> {

    /**
     * Finds a flight by its flight number.
     *
     * @param flightNumber the flight number to search for
     * @return an Optional containing the found Flight, or an empty Optional if no flight is found
     */
    Optional<Flight> findByFlightNumber(String flightNumber);

    /**
     * Finds a flight by its departure time.
     *
     * @param departureTime the departure time to search for
     * @return an Optional containing the found Flight, or an empty Optional if no flight is found
     */
    Optional<Flight> findByDepartureTime(String departureTime);

    /**
     * Finds a flight by its arrival time.
     *
     * @param arrivalTime the arrival time to search for
     * @return an Optional containing the found Flight, or an empty Optional if no flight is found
     */
    Optional<Flight> findByArrivalTime(String arrivalTime);
}

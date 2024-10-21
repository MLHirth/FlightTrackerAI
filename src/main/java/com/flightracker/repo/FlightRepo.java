package com.flightracker.repo;

import com.flightracker.domain.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightRepo extends JpaRepository<Flight, String> {
    Optional<Flight> findByFlightNumber(String flightNumber);
    Optional<Flight> findByDepartureTime(String departureTime);
    Optional<Flight> findByArrivalTime(String arrivalTime);
}

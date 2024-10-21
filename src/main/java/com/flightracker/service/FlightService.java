package com.flightracker.service;

import com.flightracker.domain.Flight;
import com.flightracker.repo.FlightRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.flightracker.constant.Constant.PASS_DIRECTORY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
@Transactional(rollbackOn = Exception.class)
@RequiredArgsConstructor
public class FlightService {
    private final FlightRepo flightRepo;

    public Page<Flight> getAllFlights(int page, int size) {
        return flightRepo.findAll(PageRequest.of(page, size, Sort.by("flightNumber")));
    }

    public Flight getFlight(String flightNumber) {
        return flightRepo.findById(flightNumber).orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    public Flight saveFlight(Flight flight) {
        log.info("Saving flight {}", flight);
        return flightRepo.save(flight);
    }

    public void deleteFlight(Flight flight) {
        log.info("Deleting flight {}", flight.getFlightNumber());
        flightRepo.delete(flight);
    }

    public String uploadBoardingPass(String flightNumber, MultipartFile file) {
        log.info("Uploading boarding pass for flight {}", flightNumber);
        Flight flight = getFlight(flightNumber);
        String BoardingPass = BoardingPassProcessing.apply(flightNumber, file);
        flight.setBoardingPass(BoardingPass);
        flightRepo.save(flight);
        return BoardingPass;
    }

    private final Function<String, String> fileExtension = filename -> Optional.of(filename).filter(name -> name.contains("."))
            .map(name -> "." + name.substring(filename.lastIndexOf(".") + 1)).orElse(".png");

    private final BiFunction<String, MultipartFile, String> BoardingPassProcessing = (flightNumber, image) -> {
        String filename = flightNumber + fileExtension.apply(image.getOriginalFilename());
        try {
            Path PassStorageLocation = Paths.get(PASS_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(PassStorageLocation)) {
                Files.createDirectory(PassStorageLocation);
            }
            Files.copy(image.getInputStream(), PassStorageLocation.resolve(filename), REPLACE_EXISTING);
            return ServletUriComponentsBuilder.fromCurrentContextPath().path("/flight/image/" + filename).toUriString();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw new RuntimeException("Boarding pass processing failed");
        }
    };
}

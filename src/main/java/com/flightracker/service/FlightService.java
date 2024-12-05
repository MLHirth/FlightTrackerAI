package com.flightracker.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static com.flightracker.constant.Constant.PASS_DIRECTORY;
import com.flightracker.domain.Flight;
import com.flightracker.repo.FlightRepo;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;

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

    public Mat FileToMat(MultipartFile file) {
        try {
            InputStream inputStream = new ByteArrayInputStream(file.getBytes());
            Mat image = Imgcodecs.imdecode(new MatOfByte(inputStream.readAllBytes()), Imgcodecs.IMREAD_COLOR);
            return image;
        } catch (Exception exception) {
            log.error("FileToMat didn't work", exception);
            return null;
        }
    }

    public String scanBoardingPass(String flightNumber, MultipartFile file) {
        try {
            // Convert MultipartFile to Mat
            Mat image = FileToMat(file);

            // Process the image (example: resize and convert to grayscale)
            // Mat processedImage = new Mat();
            // Imgproc.resize(image, processedImage, new Size(800, 600));
            // Imgproc.cvtColor(processedImage, processedImage, Imgproc.COLOR_BGR2GRAY);

            // Perform OCR or other processing (this is just a placeholder)
            String extractedData = performOCR(image);

            // Save the processed image or extracted data as needed
            String filename = flightNumber + "_processed.png";
            Path storageLocation = Paths.get(PASS_DIRECTORY).toAbsolutePath().normalize();
            if (!Files.exists(storageLocation)) {
                Files.createDirectory(storageLocation);
            }
            Imgcodecs.imwrite(storageLocation.resolve(filename).toString(), image);

            return extractedData;
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            return null;
        }
    }

    private String performOCR(Mat image) {
        // Define the region of interest in the image
        int x = 100; // starting x coordinate
        int y = 50;  // starting y coordinate
        int width = 300; // width of the ROI
        int height = 100; // height of the ROI

        // Extract the roi from the image
        Mat roi = new Mat(image, new org.opencv.core.Rect(x, y, width, height));

        return readImage(roi);
    }

    private String readImage(Mat image) {
        // Convert the Mat object to a byte array
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", image, matOfByte);
        byte[] byteArray = matOfByte.toArray();

        // Initialize Tesseract OCR
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata"); // Path to tessdata directory

        // Perform OCR on the image
        String extractedFile = "";
        try {
           extractedFile = tesseract.doOCR(matToBufferedImage(image));
        } catch (Exception exception) {
            log.error("could not extract: " + extractedFile, exception);
        }
        return extractedFile;
    }

    private BufferedImage matToBufferedImage(Mat mat){
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        try {
            return ImageIO.read(inputStream);
        } catch (Exception exception) {
            log.error("could not read the image", exception);
            return null;
        }
    }

    private String QRCodeProcessing(BufferedImage bufferedImage) {
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception exception) {
            log.error("QR/Barcode scanning failed", exception);
            return null;
        }
    }

    public String scanQRCode(MultipartFile file) {
        try {
            return QRCodeProcessing(matToBufferedImage(FileToMat(file)));
        } catch (Exception exception) {
            log.error("qr code couldn't be processed", exception);
            return("Couldn't be processed");
        }
    }

    public Flight findFlightUsingQR(MultipartFile file) {
        return getFlight(scanQRCode(file));
    }
}

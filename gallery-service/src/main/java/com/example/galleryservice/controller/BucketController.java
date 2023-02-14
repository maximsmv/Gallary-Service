package com.example.galleryservice.controller;

import com.example.galleryservice.exception.BucketNotFoundException;
import com.example.galleryservice.model.Bucket;
import com.example.galleryservice.repository.BucketRepository;
import com.example.galleryservice.payload.ErrorResponse;
import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/")
public class BucketController {

    @Autowired
    private Environment env;
    @Autowired
    private BucketRepository bucketRepository;

    @RequestMapping("/")
    public String home() {
        String home = "Gallery-Service running at port: " + env.getProperty("local.server.port");
        log.info(home);
        return home;
    }

    @GetMapping(path = "/show")
    public Flux<Bucket> getAllEmployeesList() {
        log.info("Get data from database (Feign Client on User-Service side)");
        return bucketRepository.findAll();
    }

    @GetMapping("/data")
    public Flux<Bucket> data() {
        log.info("Get data from database (RestTemplate on User-Service side)");
        return bucketRepository.findAll();
    }

    @GetMapping("/getAll")
    public Flux<Bucket> getAllBuckets() {
        return bucketRepository.findAll();
    }

    @PostMapping("/create")
    public Mono<Bucket> createBucket(@Valid @RequestBody Bucket bucket) {
        return bucketRepository.save(bucket);
    }

    @GetMapping("/get/{id}")
    public Mono<ResponseEntity<Bucket>> getBucketById(@PathVariable(value = "id") String bucketId) {
        return bucketRepository.findById(bucketId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/update/{id}")
    public Mono<ResponseEntity<Bucket>> updateBucket(@PathVariable(value = "id") String bucketId,
                                                     @Valid @RequestBody Bucket bucket) {
        return bucketRepository.findById(bucketId)
                .flatMap(existingBucket -> {
                    existingBucket.setDescription(bucket.getDescription());
                    existingBucket.setImageLink(bucket.getImageLink());
                    return bucketRepository.save(existingBucket);
                })
                .map(updateBucket -> new ResponseEntity<>(updateBucket, HttpStatus.OK))
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<Void>> deleteBucket(@PathVariable(value = "id") String bucketId) {

        return bucketRepository.findById(bucketId)  // First, you search the Bucket you want to delete.
                .flatMap(existingBucket ->
                        bucketRepository.delete(existingBucket)  // Next, you delete and return 200 OK to show your delete was successful
                                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));  // or you return 404 NOT FOUND to say the Bucket was not found
    }

    @DeleteMapping("/deleteAllBuckets")
    public Mono<Void> deleteAllBuckets(){
        return bucketRepository.deleteAll();
    }

    // Buckets are Sent to the client as Server Sent Events
    @GetMapping(value = "/stream/buckets", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Bucket> streamAllBuckets() {
        return bucketRepository.findAll();
    }

    // Get default value every 1 second
    @GetMapping(value = "/stream/buckets/default", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Bucket> emitBuckets() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(val -> new Bucket( "" + val, "Python", "default theme", 0, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS2f2NovvIAZjv9jGeSmzXnWnkiIXZX2VR7i2e-v_V756pWxFSS"));
    }

    // Get all Bucket from the database (every 1 second you will receive 1 record from the DB)
    @GetMapping(value = "/stream/buckets/delay", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Bucket> streamAllBucketsDelay() {
        log.info("Get data from database (WebClient on User-Service side)");
        return bucketRepository.findAll().delayElements(Duration.ofSeconds(2));
    }

    // Exception Handling Examples (These can be put into a @ControllerAdvice to handle exceptions globally)
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity handleDuplicateKeyException(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("A Bucket with the same title already exists"));
    }

    @ExceptionHandler(BucketNotFoundException.class)
    public ResponseEntity handleBucketNotFoundException(BucketNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

}

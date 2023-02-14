package com.example.galleryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GalleryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GalleryServiceApplication.class, args);
	}

}

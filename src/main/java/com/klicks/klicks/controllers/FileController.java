package com.klicks.klicks.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.klicks.klicks.entities.ExtraGear;
import com.klicks.klicks.entities.StandartGear;
import com.klicks.klicks.entities.Token;
import com.klicks.klicks.payload.UploadFileResponse;
import com.klicks.klicks.repositories.ExtraGearRepository;
import com.klicks.klicks.repositories.StandartGearRepository;
import com.klicks.klicks.repositories.TokenRepository;
import com.klicks.klicks.service.FileStorageService;
import com.klicks.klicks.validation.Validation;

@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
public class FileController {
	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	private FileStorageService fileStorageService;

	private StandartGearRepository standartGearRepository;

	private ExtraGearRepository extraGearRepository;

	private TokenRepository tokenRepository;

	public FileController(FileStorageService fileStorageService, StandartGearRepository standartGearRepository,
			ExtraGearRepository extraGearRepository, TokenRepository tokenRepository) {
		super();
		this.fileStorageService = fileStorageService;
		this.standartGearRepository = standartGearRepository;
		this.extraGearRepository = extraGearRepository;
		this.tokenRepository = tokenRepository;
	}

	@PostMapping("/uploadFile")
	public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "X-KLICKS-AUTH") String alphanumeric) {
		Token token = tokenRepository.findByAlphanumeric(alphanumeric);
		Validation.validateTokenForAdmin(token);
		String fileName = fileStorageService.storeFile(file);

		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/downloadFile/")
				.path(fileName).toUriString();

		return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
	}

	@PostMapping("/uploadMultipleFiles")
	public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files,
			@RequestHeader(value = "X-KLICKS-AUTH") String alphanumeric) {
		Token token = tokenRepository.findByAlphanumeric(alphanumeric);
		Validation.validateTokenForAdmin(token);
		return Arrays.asList(files).stream().map(file -> uploadFile(file, alphanumeric)).collect(Collectors.toList());
	}

	@GetMapping("/downloadFile/{fileName:.+}")
	public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request,
			@RequestHeader(value = "X-KLICKS-AUTH") String alphanumeric) {
		Token token = tokenRepository.findByAlphanumeric(alphanumeric);
		Validation.validateTokenForAdmin(token);
		Resource resource = fileStorageService.loadFileAsResource(fileName);
		String contentType = null;
		try {
			contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.info("Could not determine file type.");
		}
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@PostMapping("/savePhotoLinkStandart/{gearId}")
	public void savePhotoLinkStandart(@PathVariable int gearId, @RequestBody String photoLink,
			@RequestHeader(value = "X-KLICKS-AUTH") String alphanumeric) {
		Token token = tokenRepository.findByAlphanumeric(alphanumeric);
		Validation.validateTokenForAdmin(token);
		StandartGear gear = standartGearRepository.findById(gearId);
		Validation.validateStandartgear(gear);
		gear.setPhotoLink(photoLink);
		standartGearRepository.save(gear);
	}

	@PostMapping("/savePhotoLinkExtra/{gearId}")
	public void savePhotoLinkExtra(@PathVariable int gearId, @RequestBody String photoLink,
			@RequestHeader(value = "X-KLICKS-AUTH") String alphanumeric) {
		Token token = tokenRepository.findByAlphanumeric(alphanumeric);
		Validation.validateTokenForAdmin(token);
		ExtraGear gear = extraGearRepository.findById(gearId);
		Validation.validateExtragear(gear);
		gear.setPhotoLink(photoLink);
		extraGearRepository.save(gear);
	}

}

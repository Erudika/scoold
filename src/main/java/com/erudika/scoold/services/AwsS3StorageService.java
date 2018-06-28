package com.erudika.scoold.services;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class AwsS3StorageService implements StorageService {
	public static final Logger logger = LoggerFactory.getLogger(AwsS3StorageService.class);

	private String endpoint = "";
	private String awsId = "";
	private String awsKey = "";

	public String store(MultipartFile file) {
		String bucketName = "ask-img-bucket";

		String name = putObject(bucketName, file);
		String url = generateUrl(bucketName, name);

		return url;
	}

	public String generateUrl(String bucketName, String filename) {
		return endpoint + "/" + bucketName + "/" + filename;
	}

	public String putObject(String bucketName, MultipartFile file) {
		AmazonS3 conn = createClient();
		String originalFilename = file.getOriginalFilename();
		String extension = FilenameUtils.getExtension(originalFilename);
		String name = UUID.randomUUID().toString() + "." + extension;

		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + originalFilename);
			}
			try (InputStream inputStream = file.getInputStream()) {
				ObjectMetadata objectMetadata = new ObjectMetadata();
				objectMetadata.setContentLength(file.getSize());
				objectMetadata.setContentType(file.getContentType());
				conn.putObject(bucketName, name, inputStream, objectMetadata);
				logger.info("Put object to S3 storage: " + name);
			}
		} catch (IOException e) {
			throw new StorageException("Failed to store file " + originalFilename, e);
		}

		conn.setObjectAcl(bucketName, name, CannedAccessControlList.PublicRead);
		logger.info("Make S3 object public: " + name);

		return name;
	}

	private AmazonS3 createClient() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsId, awsKey);

		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setProtocol(Protocol.HTTP);
		clientConfig.setSignerOverride("AWSS3V4SignerType");

		AmazonS3ClientBuilder.standard().setEndpointConfiguration(
				new AwsClientBuilder.EndpointConfiguration(endpoint, "mexico"));


		AmazonS3 conn = new AmazonS3Client(awsCreds, clientConfig);
		conn.setEndpoint(endpoint);
		conn.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
		return conn;
	}

	public void listBucketContent(String bucketName) {
		AmazonS3 client = createClient();
		ObjectListing objects = client.listObjects(bucketName);
		do {
			for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
				System.out.println(
						objectSummary.getKey() + "\t" +
						objectSummary.getSize() + "\t" +
						StringUtils.fromDate(objectSummary.getLastModified()));
			}
			objects = client.listNextBatchOfObjects(objects);
		} while (objects.isTruncated());
	}

	public List<Bucket> listBuckets() {
		return createClient().listBuckets();
	}

	public Bucket getBucket(String bucketName) {
		List<Bucket> buckets = listBuckets();
		for (Bucket bucket : buckets) {
			if (bucketName.equals(bucket.getName())) {
				return bucket;
			}
		}
		throw new RuntimeException(String.format("Bucket: %s can't be found.", bucketName));
	}
}

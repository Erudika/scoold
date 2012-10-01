
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.CreateInvalidationResult;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author alexb
 */
public class Run {

	private static final String bucket = "com.scoold.files";
	private static final String distributionID = System.getProperty("awscfdist");
	protected static final String ACCESSKEY = System.getProperty("awsaccesskey");
	protected static final String SECRETKEY = System.getProperty("awssecretkey");
	private static final Logger logger = Logger.getLogger(Run.class.getName());
	private static final BasicAWSCredentials awsCredentials = new BasicAWSCredentials(ACCESSKEY, SECRETKEY);
	private static AmazonS3Client s3 = new AmazonS3Client(awsCredentials);
	private static AmazonCloudFrontClient cf = new AmazonCloudFrontClient(awsCredentials);
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		if(args.length > 0){
			if(args[0].equals("all")){
				invalidateCDNCache(null);
			}else if (args.length == 2) {
				String sumsfile = args[0].substring(args[0].lastIndexOf("/") + 1);
				boolean changesFound = false;
				S3Object chksms = getS3Object(sumsfile);
				ArrayList<String> invalidate = new ArrayList<String>();
				Map<String, String> filepaths = readFile(args[1]);
				Map<String, String> localSums = readFile(args[0]);
				Map<String, String> remoteSums = readInputStream((chksms != null) ? 
						chksms.getObjectContent() : null);

				for (Map.Entry<String, String> entry : localSums.entrySet()) {
					String lfile = entry.getKey();						
					String lsum = entry.getValue();
					String rsum = remoteSums.get(lfile);
					String fullpath = filepaths.get(lfile);

					if(fullpath != null && !fullpath.trim().isEmpty()){
						if((rsum != null && !lsum.equals(rsum)) || rsum == null){
							if (rsum == null) {
								// reupload file
								System.out.println("found new file "+lfile);
							} else {
								// upload new version and mark for invalidation
								System.out.println("found changes in "+lfile);
							}
							
							uploadFile(lfile, fullpath, true, true);
							invalidate.add("/"+lfile);							
							changesFound = true;
						}
					}
				}

				if(!invalidate.isEmpty()){
					invalidateCDNCache(invalidate);
				}else{
					System.out.println("no changes found - nothing to upload or invalidate.");
				}
				
				// finally upload new checksums.txt file 
				if(changesFound) uploadFile(sumsfile, args[0], true, false);
			}else{
				System.out.println("USAGE: jar checksums.txt filepaths.txt");
			}
		}
	}

	private static boolean invalidateCDNCache(List<String> keys) {
		boolean ok = false;		
		try {
			if (keys == null || keys.isEmpty()) {
				ObjectListing ol = s3.listObjects(bucket);
				List<S3ObjectSummary> list = ol.getObjectSummaries();
				keys = new ArrayList<String>();				
				for (S3ObjectSummary s3ObjectSummary : list) {
					keys.add("/"+s3ObjectSummary.getKey());
				}
			}		
			System.out.println("invalidating " + keys);
			
			Paths paths = new Paths().withItems(keys);
			InvalidationBatch batch = new InvalidationBatch(paths, "" + System.currentTimeMillis());
			CreateInvalidationResult cir = cf.createInvalidation(new CreateInvalidationRequest(distributionID, batch));
			System.out.println("invalidation request status: " + cir.getInvalidation().getStatus());
			ok = true;
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return ok;
	}
	
	private static S3Object getS3Object(String name){
		S3Object so = null;
		try {
			GetObjectRequest gor = new GetObjectRequest(bucket, name);
			so = s3.getObject(gor);
		} catch (Exception e) {}
		return so;
	}
	
	private static void uploadFile(String name, String path, boolean publik, boolean gzip){
		if(name == null || name.isEmpty() || path == null || path.isEmpty()) return;
		PutObjectRequest por = new PutObjectRequest(bucket, name, new File(path));
		if (publik) por.setCannedAcl(CannedAccessControlList.PublicRead);
		por.setStorageClass(StorageClass.ReducedRedundancy);
		ObjectMetadata om = new ObjectMetadata();
		om.setCacheControl("max-age=605000, must-revalidate");
		if (gzip) om.setContentEncoding("gzip");
		por.setMetadata(om);
		s3.putObject(por);
	}
	
	private static Map<String, String> readInputStream(InputStream stream){
		Map<String, String> map = new HashMap<String, String>();
		if(stream == null) return map;
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(stream, writer, "UTF-8");
			String theString = writer.toString();
			for (String line : theString.split("[\\r\\n]")) {
				String[] s = line.split("\\s");
				if(s.length == 2 && !s[0].trim().isEmpty() && !s[1].trim().isEmpty()){
					map.put(s[0], s[1]);
				}
			}
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		} finally {
			IOUtils.closeQuietly(stream);
			IOUtils.closeQuietly(writer);
		}
		return map;
	}
	
	private static Map<String, String> readFile(String name){
		Map<String, String> map = new HashMap<String, String>();
		if(name == null || name.isEmpty()) return map;
		try {
			File file = new File(name);
			List<String> lines = FileUtils.readLines(file, "UTF-8");
			for (String line : lines) {
				line = line.trim();
				String[] s = line.split("\\s");
				if(s.length == 2 && !s[0].trim().isEmpty() && !s[1].trim().isEmpty()){
					map.put(s[0], s[1]);
				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return map;
	}	
}

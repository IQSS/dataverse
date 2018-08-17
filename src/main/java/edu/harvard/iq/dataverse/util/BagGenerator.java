package edu.harvard.iq.dataverse.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

public class BagGenerator {

	private static final Logger log = Logger.getLogger(BagGenerator.class);
	// ToDo - make #threads configurable
	private ParallelScatterZipCreator scatterZipCreator = new ParallelScatterZipCreator(
			Executors.newFixedThreadPool(4));
	private ScatterZipOutputStream dirs = null;

	private JsonArray aggregates = null;
	private ArrayList<String> resourceIndex = null;
	private Boolean[] resourceUsed = null;
	private HashMap<String, String> pidMap = new LinkedHashMap<String, String>();
	private HashMap<String, String> shaMap = new LinkedHashMap<String, String>();

	private int timeout = 300;
	private RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
			.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
	private static HttpClientContext localContext = HttpClientContext.create();
	protected CloseableHttpClient client;
	private PoolingHttpClientConnectionManager cm = null;

	private ChecksumType hashtype = null;
	private boolean ignorehashes = false;

	private long dataCount = 0l;
	private long totalDataSize = 0l;
	private long maxFileSize = 0l;
	private Set<String> mimetypes = new TreeSet<String>();

	private String bagID = null;
	private String bagPath = "/tmp";
	String bagName = null;
	
	private String apiKey = null;

	private JsonObject oremap;
	private JsonObject aggregation;

	private boolean usetemp = false;

	static PrintWriter pw = null;

	public BagGenerator(JsonObject oremap) {
		this.oremap = oremap;
		try {
			// SSLContext sslContext;

			// sslContext = SSLContext.getInstance("TLSv1.2");

			// sslContext.init(null, null, null);
			SSLContextBuilder builder = new SSLContextBuilder();
			try {
				builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(builder.build(),
					NoopHostnameVerifier.INSTANCE);

			Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslConnectionFactory).build();
			cm = new PoolingHttpClientConnectionManager(registry);

			cm.setDefaultMaxPerRoute(4);
			cm.setMaxTotal(4 > 20 ? 4 : 20);

			client = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config).build();
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			// TODO Auto-generated catch block
			log.warn("Aint gonna work");
			e.printStackTrace();
		}
	}

	public void setIgnoreHashes(boolean val) {
		ignorehashes = val;
	}

	public static void println(String s) {
		System.out.println(s);
		System.out.flush();
		if (pw != null) {
			pw.println(s);
			pw.flush();
		}
		return;
	}

	public static void main(String args[]) throws Exception {
		try {
			File outputFile = new File("QDRBagItLog_" + System.currentTimeMillis() + ".txt");
			try {
				pw = new PrintWriter(new FileWriter(outputFile));
			} catch (Exception e) {
				println(e.getMessage());
			}
			String mapFileName = null;
			String apiKey = null;
			// go through arguments
			for (String arg : args) {
				// First non-flag arg is the server URL
				if (mapFileName == null) {
					println("MapFile: " + arg);
					mapFileName = arg;
				}
				if (apiKey == null) {
					println("APIKey: " + arg);
					apiKey = arg;
				}
			}
			// Read from File to String
			JsonObject oremap = new JsonObject();

			try {
				JsonParser parser = new JsonParser();
				JsonElement jsonElement = parser.parse(new FileReader(mapFileName));
				oremap = jsonElement.getAsJsonObject();
			} catch (FileNotFoundException e) {
				log.warn("Couldn't find " + mapFileName);
			} 

			BagGenerator bg = new BagGenerator(oremap);
			bg.setBagPath(".");
			bg.generateBag("testBag", false);
			if (pw != null) {
				pw.flush();
				pw.close();
			}
		} catch (Exception e) {
			println(e.getLocalizedMessage());
			e.printStackTrace(pw);
			pw.flush();
			System.exit(1);
		}

	}

	/*
	 * Full workflow to generate new BagIt bag from ORE Map Url and to write the bag
	 * to the provided output stream (Ex: File OS, FTP OS etc.).
	 * 
	 * @return success true/false
	 */
	public boolean generateBag(OutputStream outputStream) throws Exception {
		log.info("Generating: Bag to the Future!");

		File tmp = File.createTempFile("qdr-scatter-dirs", "tmp");
		dirs = ScatterZipOutputStream.fileBased(tmp);

		aggregation = oremap.getAsJsonObject(JsonLDTerm.ore("describes").getLabel());

		bagID = aggregation.get("@id").getAsString() + aggregation.get(JsonLDTerm.schemaOrg("version").getLabel());
		try {
			// Create valid filename from identifier and extend path with
			// two levels of hash-based subdirs to help distribute files
			bagName = getValidName(bagID);
		} catch (Exception e) {
			log.error("Couldn't create valid filename: " + e.getLocalizedMessage());
			return false;
		}
		// Create data dir in bag, also creates parent bagName dir
		String currentPath = "data/";
		createDir(currentPath);

		aggregates = aggregation.getAsJsonArray(JsonLDTerm.ore("aggregates").getLabel());

		if (aggregates != null) {
			// Add container and data entries
			// Setup global index of the aggregation and all aggregated
			// resources by Identifier
			resourceIndex = indexResources(bagID, aggregates);
			// Setup global list of succeed(true), fail(false), notused
			// (null) flags
			resourceUsed = new Boolean[aggregates.size() + 1];
			// Process current container (the aggregation itself) and its
			// children
			processContainer(aggregation, currentPath);
		}
		// Create mainifest files
		// pid-mapping.txt - a DataOne recommendation to connect ids and
		// in-bag path/names
		StringBuffer pidStringBuffer = new StringBuffer();
		boolean first = true;
		for (Entry<String, String> pidEntry : pidMap.entrySet()) {
			if (!first) {
				pidStringBuffer.append("\r\n");
			} else {
				first = false;
			}
			String path = pidEntry.getValue();
			pidStringBuffer.append(pidEntry.getKey() + " " + path);
		}
		createFileFromString("pid-mapping.txt", pidStringBuffer.toString());
		// Hash manifest - a hash manifest is required
		// by Bagit spec
		StringBuffer sha1StringBuffer = new StringBuffer();
		first = true;
		for (Entry<String, String> sha1Entry : shaMap.entrySet()) {
			if (!first) {
				sha1StringBuffer.append("\r\n");
			} else {
				first = false;
			}
			String path = sha1Entry.getKey();
			sha1StringBuffer.append(sha1Entry.getValue() + " " + path);
		}
		if (!(hashtype == null)) {
			String manifestName = "manifest-";
			if (hashtype.equals(DataFile.ChecksumType.SHA1)) {
				manifestName = manifestName + "sha1.txt";
			} else if (hashtype.equals(DataFile.ChecksumType.SHA256)) {
				manifestName = manifestName + "sha256.txt";
			} else if (hashtype.equals(DataFile.ChecksumType.SHA512)) {
				manifestName = manifestName + "sha512.txt";
			} else if (hashtype.equals(DataFile.ChecksumType.MD5)) {
				manifestName = manifestName + "md5.txt";
			} else {
				log.warn("Unsupported Hash type: " + hashtype);
			}
			createFileFromString(manifestName, sha1StringBuffer.toString());
		} else {
			log.warn("No Hash values sent - Bag File does not meet BagIT specification requirement");
		}
		// bagit.txt - Required by spec
		createFileFromString("bagit.txt", "BagIt-Version: 1.0\r\nTag-File-Character-Encoding: UTF-8");

		aggregation.addProperty(JsonLDTerm.totalSize.getLabel(), totalDataSize);
		aggregation.addProperty(JsonLDTerm.fileCount.getLabel(),dataCount);
		JsonArray mTypes= new JsonArray();
		for(String mt: mimetypes) {
			mTypes.add(new JsonPrimitive(mt));
		}
		aggregation.add(JsonLDTerm.dcTerms("format").getLabel(), mTypes);
		aggregation.addProperty(JsonLDTerm.maxFileSize.getLabel(), maxFileSize);
		// Serialize oremap itself
		// FixMe - add missing hash values if needed and update context
		// (read and cache files or read twice?)
		createFileFromString("oremap.jsonld.txt", oremap.toString());

		// Add a bag-info file
		createFileFromString("bag-info.txt", generateInfoFile(oremap));

		log.info("Creating bag: " + bagName);

		ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputStream);

		// Add all the waiting contents - dirs created first, then data
		// files
		// are retrieved via URLs in parallel (defaults to one thread per
		// processor)
		// directly to the zip file
		log.debug("Starting write");
		writeTo(zipArchiveOutputStream);
		log.info("Zipfile Written");
		// Finish
		zipArchiveOutputStream.close();
		log.debug("Closed");

		// Validate oremap - all entries are part of the collection
		for (int i = 0; i < resourceUsed.length; i++) {
			Boolean b = resourceUsed[i];
			if (b == null) {
				log.warn("Problem: " + pidMap.get(resourceIndex.get(i)) + " was not used");
			} else if (!b) {
				log.warn("Problem: " + pidMap.get(resourceIndex.get(i)) + " was not included successfully");
			} else {
				// Successfully included - now check for hash value and
				// generate if needed
				if (i > 0) { // Not root container
					if (!shaMap.containsKey(pidMap.get(resourceIndex.get(i)))) {

						if (!childIsContainer(aggregates.get(i - 1).getAsJsonObject()))
							log.warn("Missing sha1 hash for: " + resourceIndex.get(i));
						// FixMe - actually generate it before adding the
						// oremap
						// to the zip
					}
				}
			}

		}

		// Consider adding stats:
		// Transfer statistics to oremap for preservation - note that the #
		// files, totalsize are checked after the zip is written
		// so any error will be recorded in the zip, but caught in the log.
		// Other elements are not curently checked.
		// JsonObject aggStats = ((JsonObject) pubRequest.get("Aggregation
		// Statistics"));
		// aggregation.put("Aggregation Statistics", aggStats);

		return true;

	}

	public boolean generateBag(String bagName, boolean temp) {
		usetemp = temp;
		FileOutputStream bagFileOS = null;
		try {
			File origBagFile = getBagFile(bagName);
			File bagFile = origBagFile;
			if (usetemp) {
				bagFile = new File(bagFile.getAbsolutePath() + ".tmp");
				log.debug("Writing to: " + bagFile.getAbsolutePath());
			}
			// Create an output stream backed by the file
			bagFileOS = new FileOutputStream(bagFile);
			if (generateBag(bagFileOS)) {
				validateBagFile(bagFile);
				if (usetemp) {
					log.debug("Moving tmp zip");
					origBagFile.delete();
					bagFile.renameTo(origBagFile);
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error("Bag Exception: ", e);
			e.printStackTrace();
			log.warn("Failure: Processing failure during Bagit file creation");
			return false;
		} finally {
			IOUtils.closeQuietly(bagFileOS);
		}
	}

	public void validateBag(String bagId) {
		log.info("Validating Bag");
		ZipFile zf = null;
		InputStream is = null;
		try {
			zf = new ZipFile(getBagFile(bagId));
			ZipArchiveEntry entry = zf.getEntry(getValidName(bagId) + "/manifest-sha1.txt");
			if (entry != null) {
				log.info("SHA1 hashes used");
				hashtype = DataFile.ChecksumType.SHA1;
				is = zf.getInputStream(entry);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = br.readLine();
				while (line != null) {
					log.debug("Hash entry: " + line);
					int breakIndex = line.indexOf(' ');
					String hash = line.substring(0, breakIndex);
					String path = line.substring(breakIndex + 1);
					log.debug("Adding: " + path + " with hash: " + hash);
					shaMap.put(path, hash);
					line = br.readLine();
				}
				IOUtils.closeQuietly(is);

			} else {
				entry = zf.getEntry(getValidName(bagId) + "/manifest-sha512.txt");
				if (entry != null) {
					log.info("SHA512 hashes used");
					hashtype = DataFile.ChecksumType.SHA512;
					is = zf.getInputStream(entry);
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String line = br.readLine();
					while (line != null) {
						int breakIndex = line.indexOf(' ');
						String hash = line.substring(0, breakIndex);
						String path = line.substring(breakIndex + 1);
						shaMap.put(path, hash);
						line = br.readLine();
					}
					IOUtils.closeQuietly(is);
				} else {
					entry = zf.getEntry(getValidName(bagId) + "/manifest-sha256.txt");
					if (entry != null) {
						log.info("SHA256 hashes used");
						hashtype = DataFile.ChecksumType.SHA256;
						is = zf.getInputStream(entry);
						BufferedReader br = new BufferedReader(new InputStreamReader(is));
						String line = br.readLine();
						while (line != null) {
							int breakIndex = line.indexOf(' ');
							String hash = line.substring(0, breakIndex);
							String path = line.substring(breakIndex + 1);
							shaMap.put(path, hash);
							line = br.readLine();
						}
						IOUtils.closeQuietly(is);
					} else {
						entry = zf.getEntry(getValidName(bagId) + "/manifest-md5.txt");
						if (entry != null) {
							log.info("MD5 hashes used");
							hashtype = DataFile.ChecksumType.MD5;
							is = zf.getInputStream(entry);
							BufferedReader br = new BufferedReader(new InputStreamReader(is));
							String line = br.readLine();
							while (line != null) {
								int breakIndex = line.indexOf(' ');
								String hash = line.substring(0, breakIndex);
								String path = line.substring(breakIndex + 1);
								shaMap.put(path, hash);
								line = br.readLine();
							}
							IOUtils.closeQuietly(is);
						}
					}
				}
			}
			log.info("HashMap Map contains: " + shaMap.size() + " entries");
			checkFiles(shaMap, zf);
		} catch (IOException io) {
			log.error("Could not validate Hashes", io);
		} catch (Exception e) {
			log.error("Could not validate Hashes", e);
		}
		return;
	}

	public File getBagFile(String bagID) throws Exception {

		String bagPath = Paths.get(getBagPath()).toString();
		// Create the bag file on disk
		File parent = new File(bagPath);
		if (!parent.exists()) {
			parent.mkdirs();
		}
		// Create known-good filename
		bagName = getValidName(bagID);
		File bagFile = new File(bagPath, bagName + ".zip");
		log.info("BagPath: " + bagFile.getAbsolutePath());
		// Create an output stream backed by the file
		return bagFile;
	}

	private void validateBagFile(File bagFile) throws IOException {
		// Run a confirmation test - should verify all files and hashes
		ZipFile zf = new ZipFile(bagFile);
		// Check files calculates the hashes and file sizes and reports on
		// whether hashes are correct
		// The file sizes are added to totalDataSize which is compared with the
		// stats sent in the request
		checkFiles(shaMap, zf);

		log.info("Data Count: " + dataCount);
		log.info("Data Size: " + totalDataSize);
		/*
		 * ToDo // Check stats if (pubRequest.getJSONObject("Aggregation Statistics").
		 * getLong("Number of Datasets") != dataCount) {
		 * log.warn("Request contains incorrect data count: should be: " + dataCount); }
		 * // Total size is calced during checkFiles if
		 * (pubRequest.getJSONObject("Aggregation Statistics").getLong("Total Size") !=
		 * totalDataSize) {
		 * log.warn("Request contains incorrect Total Size: should be: " +
		 * totalDataSize); }
		 */
		zf.close();
	}

	public static String getValidName(String bagName) {
		// Create known-good filename - no spaces, no file-system separators.
		return bagName.replaceAll("\\W", "_");
	}

	private void processContainer(JsonObject item, String currentPath) {
		JsonArray children = getChildren(item);
		HashSet<String> titles = new HashSet<String>();
		String title = null;
		if (item.has(JsonLDTerm.dcTerms("Title").getLabel())) {
			title = item.get("Title").getAsString();
		} else if (item.has(JsonLDTerm.schemaOrg("name").getLabel())) {
			title = item.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString();
		}

		currentPath = currentPath + title + "/";
		int containerIndex = -1;
		try {
			createDir(currentPath);
			// Add containers to pid map and mark as 'used', but no sha1 hash
			// value
			containerIndex = getUnusedIndexOf(item.get("@id").getAsString());
			resourceUsed[containerIndex] = true;
			pidMap.put(item.get("@id").getAsString(), currentPath);
		} catch (Exception e) {
			resourceUsed[containerIndex] = false;
			e.printStackTrace();
		}
		for (int i = 0; i < children.size(); i++) {

			// Find the ith child in the overall array of aggregated
			// resources
			String childId = children.get(i).getAsString();
			log.info("Processing: " + childId);
			int index = getUnusedIndexOf(childId);
			if (resourceUsed[index] != null) {
				System.out.println("Warning: reusing resource " + index);
			}

			// Aggregation is at index 0, so need to shift by 1 for aggregates
			// entries
			JsonObject child = aggregates.get(index - 1).getAsJsonObject();
			if (childIsContainer(child)) {
				// create dir and process children
				// processContainer will mark this item as used
				processContainer(child, currentPath);
			} else {
				resourceUsed[index] = true;
				// add item
				// ToDo
				String dataUrl = child.get("@id").getAsString();
				log.info("File url: " + dataUrl);
				String childTitle = child.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString(); 
				if (titles.contains(childTitle)) {
					log.warn("**** Multiple items with the same title in: " + currentPath);
					log.warn("**** Will cause failure in hash and size validation.");
				} else {
					titles.add(childTitle);
				}
				String childPath = currentPath + childTitle;

				String childHash = null;
				if (child.has(JsonLDTerm.checksum.getLabel())) {
					ChecksumType childHashType = ChecksumType.fromString(
							child.getAsJsonObject(JsonLDTerm.checksum.getLabel()).get("@type").getAsString());
					if (hashtype != null && !hashtype.equals(childHashType)) {
						log.warn("Multiple hash values in use - not supported");
					}
					if (hashtype == null)
						hashtype = childHashType;
					childHash = child.getAsJsonObject(JsonLDTerm.checksum.getLabel()).get("@value").getAsString();
					if (shaMap.containsValue(childHash)) {
						// Something else has this hash
						log.warn("Duplicate/Collision: " + child.get("@id").getAsString() + " has SHA1 Hash: "
								+ childHash);
					}
					shaMap.put(childPath, childHash);
				}
				if ((hashtype == null) | ignorehashes) {
					// Pick sha256 when ignoring hashes or none exist
					hashtype = DataFile.ChecksumType.SHA256;
				}
				try {
					if ((childHash == null) | ignorehashes) {
						// Generate missing hashInputStream inputStream = null;
						InputStream inputStream = null;
						try {
							inputStream = getInputStreamSupplier(dataUrl).get();

							if (hashtype != null) {
								if (hashtype.equals(DataFile.ChecksumType.SHA1)) {
									childHash = DigestUtils.sha1Hex(inputStream);
								} else if (hashtype.equals(DataFile.ChecksumType.SHA256)) {
									childHash = DigestUtils.sha256Hex(inputStream);
								} else if (hashtype.equals(DataFile.ChecksumType.SHA512)) {
									childHash = DigestUtils.sha512Hex(inputStream);
								} else if (hashtype.equals(DataFile.ChecksumType.MD5)) {
									childHash = DigestUtils.md5Hex(inputStream);
								}
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							IOUtils.closeQuietly(inputStream);
						}
						if (childHash != null) {
							JsonObject childHashObject = new JsonObject();
							childHashObject.addProperty("@type", hashtype.toString());
							childHashObject.addProperty("@value", childHash);
							child.add(JsonLDTerm.checksum.getLabel(), (JsonElement) childHashObject);

							shaMap.put(childPath, childHash);
						} else {
							log.warn("Unable to calculate a " + hashtype + " for " + dataUrl);
						}
					}
					log.debug("Requesting: " + childPath + " from " + dataUrl);
					createFileFromURL(childPath, dataUrl);
					dataCount++;
					if (dataCount % 1000 == 0) {
						log.info("Retrieval in progress: " + dataCount + " files retrieved");
					}
					if (child.has(JsonLDTerm.filesize.getLabel())) {
						Long size = child.get(JsonLDTerm.filesize.getLabel()).getAsLong();
						totalDataSize += size;
						if(size>maxFileSize) {
							maxFileSize=size;
						}
					}
					if(child.has(JsonLDTerm.schemaOrg("fileFormat").getLabel())) {
						mimetypes.add(child.get(JsonLDTerm.schemaOrg("fileFormat").getLabel()).getAsString());
					}
					
				} catch (Exception e) {
					resourceUsed[index] = false;
					e.printStackTrace();
				}

				// Check for nulls!
				pidMap.put(child.get("@id").getAsString(), childPath);

			}
		}
	}

	private int getUnusedIndexOf(String childId) {
		int index = resourceIndex.indexOf(childId);
		if (resourceUsed[index] != null) {
			System.out.println("Warning: reusing resource " + index);
		}

		while (resourceUsed[index] != null) {
			int offset = index;
			index = offset + 1 + resourceIndex.subList(offset + 1, resourceIndex.size()).indexOf(childId);
		}
		System.out.println("Using index: " + index);
		if (index == -1) {
			log.error("Reused ID: " + childId + " not found enough times in resource list");
		}
		return index;
	}

	private ArrayList<String> indexResources(String aggId, JsonArray aggregates) {

		ArrayList<String> l = new ArrayList<String>(aggregates.size() + 1);
		l.add(aggId);
		for (int i = 0; i < aggregates.size(); i++) {
			log.debug("Indexing : " + i + " " + aggregates.get(i).getAsJsonObject().get("@id").getAsString());
			l.add(aggregates.get(i).getAsJsonObject().get("@id").getAsString());
		}
		log.info("Index created for " + aggregates.size() + " entries");
		return l;
	}

	private void createDir(final String name) throws IOException, ExecutionException, InterruptedException {

		ZipArchiveEntry archiveEntry = new ZipArchiveEntry(bagName + "/" + name);
		archiveEntry.setMethod(ZipEntry.DEFLATED);
		InputStreamSupplier supp = new InputStreamSupplier() {
			public InputStream get() {
				return new ByteArrayInputStream(("").getBytes());
			}
		};

		addEntry(archiveEntry, supp);
	}

	private void createFileFromString(final String relPath, final String content)
			throws IOException, ExecutionException, InterruptedException {

		ZipArchiveEntry archiveEntry = new ZipArchiveEntry(bagName + "/" + relPath);
		archiveEntry.setMethod(ZipEntry.DEFLATED);
		InputStreamSupplier supp = new InputStreamSupplier() {
			public InputStream get() {
				try {
					return new ByteArrayInputStream(content.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		};

		addEntry(archiveEntry, supp);
	}

	private void createFileFromURL(final String relPath, final String uri)
			throws IOException, ExecutionException, InterruptedException {

		ZipArchiveEntry archiveEntry = new ZipArchiveEntry(bagName + "/" + relPath);
		archiveEntry.setMethod(ZipEntry.DEFLATED);
		InputStreamSupplier supp = getInputStreamSupplier(uri);
		addEntry(archiveEntry, supp);
	}

	private void checkFiles(HashMap<String, String> shaMap, ZipFile zf) {
		ExecutorService executor = Executors.newFixedThreadPool(4);
		/*
		 * ToDo validate ValidationJob.setZipFile(zf);
		 * 
		 * ValidationJob.setBagGenerator(this);
		 * log.info("Validating hashes for zipped data files"); int i = 0; for
		 * (Entry<String, String> entry : shaMap.entrySet()) {
		 * 
		 * ValidationJob vj = new ValidationJob(entry.getValue(), entry.getKey());
		 * executor.execute(vj); i++; if (i % 1000 == 0) {
		 * log.info("Queuing Hash Validations: " + i); } }
		 * log.info("All Hash Validations Queued: " + i);
		 * 
		 * executor.shutdown(); try { while (!executor.awaitTermination(10,
		 * TimeUnit.MINUTES)) { log.debug("Awaiting completion of hash calculations.");
		 * } } catch (InterruptedException e) {
		 * log.error("Hash Calculations interrupted", e); }
		 * log.info("Hash Validations Completed");
		 */
	}

	public void addEntry(ZipArchiveEntry zipArchiveEntry, InputStreamSupplier streamSupplier) throws IOException {
		if (zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink())
			dirs.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(zipArchiveEntry, streamSupplier));
		else
			scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
	}

	public void writeTo(ZipArchiveOutputStream zipArchiveOutputStream)
			throws IOException, ExecutionException, InterruptedException {
		log.debug("Writing dirs");
		dirs.writeTo(zipArchiveOutputStream);
		dirs.close();
		log.debug("Dirs written");
		scatterZipCreator.writeTo(zipArchiveOutputStream);
		log.debug("Files written");
	}

	static final String CRLF = "\r\n";

	private String generateInfoFile(JsonObject map) {
		log.debug("Generating info file");
		StringBuffer info = new StringBuffer();

		JsonArray contactsArray = new JsonArray();
		if (aggregation.has(JsonLDTerm.contact.getLabel())) {

			JsonElement contacts = aggregation.get(JsonLDTerm.contact.getLabel());

			if (contacts.isJsonArray()) {
				for (int i = 0; i < contactsArray.size(); i++) {
					info.append("Contact-Name: ");
					JsonElement person = contactsArray.get(i);
					if (person.isJsonPrimitive()) {
						info.append(person.getAsString());
						info.append(CRLF);

					} else {
						info.append(((JsonObject) person).get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString());
						info.append(CRLF);
						if (((JsonObject) person).has(JsonLDTerm.email.getLabel())) {
							info.append("Contact-Email: ");
							info.append(((JsonObject) person).get(JsonLDTerm.email.getLabel()).getAsString());
							info.append(CRLF);
						}
					}
				}
			} else {
				info.append("Contact-Name: ");

				if (contacts.isJsonPrimitive()) {
					info.append((String) contacts.getAsString());
					info.append(CRLF);

				} else {
					JsonObject person = contacts.getAsJsonObject();

					info.append(person.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString());
					info.append(CRLF);
					if (person.has(JsonLDTerm.email.getLabel())) {
						info.append("Contact-Email: ");
						info.append(person.get(JsonLDTerm.email.getLabel()).getAsString());
						info.append(CRLF);
					}
				}

			}
		}

		info.append("Source-Organization: " + ResourceBundle.getBundle("Bundle").getString("bagit.sourceOrganization"));
		// ToDo - make configurable
		info.append(CRLF);

		info.append("Organization-Address: " + WordUtils.wrap(
				ResourceBundle.getBundle("Bundle").getString("bagit.sourceOrganizationAddress"), 78, CRLF + " ", true));
		info.append(CRLF);

		//Not a BagIt standard name
		info.append("Organization-Email: " + ResourceBundle.getBundle("Bundle").getString("bagit.sourceOrganizationEmail"));
		info.append(CRLF);

		info.append("External-Description: ");

		info.append(
				// FixMe - handle description having subfields better
				WordUtils.wrap(getSingleValue(aggregation.getAsJsonObject(JsonLDTerm.description.getLabel()),
						JsonLDTerm.text.getLabel()), 78, CRLF + " ", true));

		info.append(CRLF);

		info.append("Bagging-Date: ");
		info.append((new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime())));
		info.append(CRLF);

		info.append("External-Identifier: ");
		info.append(aggregation.get("@id").getAsString());
		info.append(CRLF);

		info.append("Bag-Size: ");
		info.append(byteCountToDisplaySize(totalDataSize));
		info.append(CRLF);

		info.append("Payload-Oxum: ");
		info.append(Long.toString(totalDataSize));
		info.append(".");
		info.append(Long.toString(dataCount));
		info.append(CRLF);

		info.append("Internal-Sender-Identifier: ");
		String catalog = ResourceBundle.getBundle("Bundle").getString("bagit.sourceOrganization") + " Catalog";
		if (aggregation.has(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel())) {
			catalog = aggregation.get(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel()).getAsString();
		}
		info.append(catalog + ":" + aggregation.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString());
		info.append(CRLF);

		return info.toString();

	}

	/**
	 * Kludge - handle when a single string is sent as an array of 1 string and, for
	 * cases where multiple values are sent when only one is expected, create a
	 * concatenated string so that information is not lost.
	 * 
	 * @param jsonObject
	 *            - the root json object
	 * @param key
	 *            - the key to find a value(s) for
	 * @return - a single string
	 */
	String getSingleValue(JsonObject jsonObject, String key) {
		String val = "";
		if (jsonObject.get(key).isJsonPrimitive()) {
			val = jsonObject.get(key).getAsString();
		} else if (jsonObject.get(key).isJsonArray()) {
			Iterator<JsonElement> iter = jsonObject.getAsJsonArray(key).iterator();
			ArrayList<String> stringArray = new ArrayList<String>();
			while (iter.hasNext()) {
				stringArray.add(iter.next().getAsString());
			}
			if (stringArray.size() > 1) {
				val = StringUtils.join((String[]) stringArray.toArray(), ",");
			} else {
				val = stringArray.get(0);
			}
			log.warn("Multiple values found for: " + key + ": " + val);
		}
		return val;
	}

	// Used in validation

	public void incrementTotalDataSize(long inc) {
		totalDataSize += inc;
	}

	public String getHashtype() {
		return hashtype.toString();
	}

	// Get's all "Has Part" children, standardized to send an array with 0,1, or
	// more elements
	private static JsonArray getChildren(JsonObject parent) {
		JsonElement o = null;
		o = parent.get(JsonLDTerm.schemaOrg("hasPart").getLabel());
		if (o == null) {
			return new JsonArray();
		} else {
			if (o.isJsonArray()) {
				return (JsonArray) o;
			} else if (o.isJsonPrimitive()) {
				JsonArray children = new JsonArray();
				children.add(o);
				return (children);
			}
			log.error("Error finding children: " + o.toString());
			return new JsonArray();
		}
	}

	// Logic to decide if this is a container -
	// first check for children, then check for source-specific type indicators
	private static boolean childIsContainer(JsonObject item) {
		if (getChildren(item).size() != 0) {
			return true;
		}
		// Also check for any indicative type
		Object o = item.get("@type");
		if (o != null) {
			if (o instanceof JSONArray) {
				// As part of an array
				for (int i = 0; i < ((JSONArray) o).length(); i++) {
					String type = ((JSONArray) o).getString(i).trim();
					if ("http://cet.ncsa.uiuc.edu/2016/Folder".equals(type)) {
						return true;
					}
				}
			} else if (o instanceof String) {
				// Or as the only type
				String type = ((String) o).trim();
				if ("http://cet.ncsa.uiuc.edu/2016/Folder".equals(type)) {
					return true;
				}
			}
		}
		return false;
	}

	public String getBagPath() {
		return bagPath;
	}

	public void setBagPath(String bagPath) {
		this.bagPath = bagPath;
	}

	private HttpGet createNewGetRequest(URI url, String returnType) {

		HttpGet request = null;

		if (apiKey != null) {
			try {
				request = new HttpGet(new URI(url.toURL().toString() + "?key=" + apiKey));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			request = new HttpGet(url);
		}
		if (returnType != null) {
			request.addHeader("accept", returnType);
		}
		return request;
	}

	InputStreamSupplier getInputStreamSupplier(final String uri) {

		return new InputStreamSupplier() {
			public InputStream get() {
				int tries = 0;
				while (tries < 5) {
					try {
						HttpGet getMap = createNewGetRequest(new URI(uri), null);
						log.trace("Retrieving " + tries + ": " + uri);
						CloseableHttpResponse response;
						response = client.execute(getMap, localContext);
						if (response.getStatusLine().getStatusCode() == 200) {
							log.trace("Retrieved: " + uri);
							return response.getEntity().getContent();
						}
						log.debug("Status: " + response.getStatusLine().getStatusCode());
						tries++;

					} catch (ClientProtocolException e) {
						tries += 5;
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// Retry if this is a potentially temporary error such
						// as a timeout
						tries++;
						log.warn("Attempt# " + tries + " : Unable to retrieve file: " + uri, e);
						if (tries == 5) {
							log.error("Final attempt failed for " + uri);
						}
						e.printStackTrace();
					} catch (URISyntaxException e) {
						tries += 5;
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				log.error("Could not read: " + uri);
				return null;
			}
		};
	}

	/**
	 * Adapted from org/apache/commons/io/FileUtils.java change to SI - add 2 digits
	 * of precision
	 */
	/**
	 * The number of bytes in a kilobyte.
	 */
	public static final long ONE_KB = 1000;

	/**
	 * The number of bytes in a megabyte.
	 */
	public static final long ONE_MB = ONE_KB * ONE_KB;

	/**
	 * The number of bytes in a gigabyte.
	 */
	public static final long ONE_GB = ONE_KB * ONE_MB;

	/**
	 * Returns a human-readable version of the file size, where the input represents
	 * a specific number of bytes.
	 *
	 * @param size
	 *            the number of bytes
	 * @return a human-readable display value (includes units)
	 */
	public static String byteCountToDisplaySize(long size) {
		String displaySize;

		if (size / ONE_GB > 0) {
			displaySize = String.valueOf(Math.round(size / (ONE_GB / 100.0d)) / 100.0) + " GB";
		} else if (size / ONE_MB > 0) {
			displaySize = String.valueOf(Math.round(size / (ONE_MB / 100.0d)) / 100.0) + " MB";
		} else if (size / ONE_KB > 0) {
			displaySize = String.valueOf(Math.round(size / (ONE_KB / 100.0d)) / 100.0) + " KB";
		} else {
			displaySize = String.valueOf(size) + " bytes";
		}
		return displaySize;
	}

}
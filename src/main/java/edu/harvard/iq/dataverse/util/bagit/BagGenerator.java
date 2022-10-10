package edu.harvard.iq.dataverse.util.bagit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.text.WordUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONArray;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;

public class BagGenerator {

    private static final Logger logger = Logger.getLogger(BagGenerator.class.getCanonicalName());

    private ParallelScatterZipCreator scatterZipCreator = null;
    private ScatterZipOutputStream dirs = null;

    private JsonArray aggregates = null;
    private ArrayList<String> resourceIndex = null;
    private Boolean[] resourceUsed = null;
    private HashMap<String, String> pidMap = new LinkedHashMap<String, String>();
    private HashMap<String, String> checksumMap = new LinkedHashMap<String, String>();

    private int timeout = 60;
    private RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000)
            .setCookieSpec(CookieSpecs.STANDARD).build();
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

    private javax.json.JsonObject oremapObject;
    private JsonObject aggregation;

    private String dataciteXml;

    private boolean usetemp = false;

    private int numConnections = 8;
    public static final String BAG_GENERATOR_THREADS = ":BagGeneratorThreads";

    private OREMap oremap;

    static PrintWriter pw = null;

    /**
     * This BagGenerator creates a BagIt version 1.0
     * (https://tools.ietf.org/html/draft-kunze-bagit-16) compliant bag that is also
     * minimally compatible with the Research Data Repository Interoperability WG
     * Final Recommendations (DOI: 10.15497/RDA00025). It works by parsing the
     * submitted OAI-ORE Map file, using the metadata therein to create required
     * BagIt metadata, and using the schema.org/sameAs entries for
     * AggregatedResources as a way to retrieve these files and store them in the
     * /data directory within the BagIt structure. The Bag is zipped. File retrieval
     * and zipping are done in parallel, using a connection pool. The required space
     * on disk is ~ n+1/n of the final bag size, e.g. 125% of the bag size for a
     * 4-way parallel zip operation.
     * @throws Exception 
     * @throws JsonSyntaxException 
     */

    public BagGenerator(OREMap oreMap, String dataciteXml) throws JsonSyntaxException, Exception {
        this.oremap = oreMap;
        this.oremapObject = oreMap.getOREMap();
                //(JsonObject) new JsonParser().parse(oreMap.getOREMap().toString());
        this.dataciteXml = dataciteXml;

        try {
            // Using Dataverse, all the URLs to be retrieved should be on the current server, so allowing self-signed certs and not verifying hostnames are useful in testing and 
            // shouldn't be a significant security issue. This should not be allowed for arbitrary OREMap sources.
            SSLContextBuilder builder = new SSLContextBuilder();
            try {
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }

            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
            		.register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionFactory).build();
            cm = new PoolingHttpClientConnectionManager(registry);

            cm.setDefaultMaxPerRoute(numConnections);
            cm.setMaxTotal(numConnections > 20 ? numConnections : 20);

            client = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config).build();

            scatterZipCreator = new ParallelScatterZipCreator(Executors.newFixedThreadPool(numConnections));
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.warning("Aint gonna work");
            e.printStackTrace();
        }
    }

    public void setIgnoreHashes(boolean val) {
        ignorehashes = val;
    }
    
    public void setDefaultCheckSumType(ChecksumType type) {
    	hashtype=type;
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

    /*
     * Full workflow to generate new BagIt bag from ORE Map Url and to write the bag
     * to the provided output stream (Ex: File OS, FTP OS etc.).
     * 
     * @return success true/false
     */
    public boolean generateBag(OutputStream outputStream) throws Exception {
        

        File tmp = File.createTempFile("qdr-scatter-dirs", "tmp");
        dirs = ScatterZipOutputStream.fileBased(tmp);
        // The oremapObject is javax.json.JsonObject and we need com.google.gson.JsonObject for the aggregation object
        aggregation = (JsonObject) new JsonParser().parse(oremapObject.getJsonObject(JsonLDTerm.ore("describes").getLabel()).toString());

        String pidUrlString = aggregation.get("@id").getAsString();
        String pidString=GlobalId.getInternalFormOfPID(pidUrlString);
        bagID = pidString + "v."
                + aggregation.get(JsonLDTerm.schemaOrg("version").getLabel()).getAsString();
        
        logger.info("Generating Bag: " + bagID);
        try {
            // Create valid filename from identifier and extend path with
            // two levels of hash-based subdirs to help distribute files
            bagName = getValidName(bagID);
        } catch (Exception e) {
            logger.severe("Couldn't create valid filename: " + e.getLocalizedMessage());
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
            resourceIndex = indexResources(aggregation.get("@id").getAsString(), aggregates);
            // Setup global list of succeed(true), fail(false), notused
            // (null) flags
            resourceUsed = new Boolean[aggregates.size() + 1];
            // Process current container (the aggregation itself) and its
            // children
            processContainer(aggregation, currentPath);
        }
        // Create manifest files
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
        createDir("metadata/");
        createFileFromString("metadata/pid-mapping.txt", pidStringBuffer.toString());
        // Hash manifest - a hash manifest is required
        // by Bagit spec
        StringBuffer sha1StringBuffer = new StringBuffer();
        first = true;
        for (Entry<String, String> sha1Entry : checksumMap.entrySet()) {
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
                logger.warning("Unsupported Hash type: " + hashtype);
            }
            createFileFromString(manifestName, sha1StringBuffer.toString());
        } else {
            logger.warning("No Hash values (no files?) sending empty manifest to nominally comply with BagIT specification requirement");
            createFileFromString("manifest-md5.txt", "");
        }
        // bagit.txt - Required by spec
        createFileFromString("bagit.txt", "BagIt-Version: 1.0\r\nTag-File-Character-Encoding: UTF-8");

        aggregation.addProperty(JsonLDTerm.totalSize.getLabel(), totalDataSize);
        aggregation.addProperty(JsonLDTerm.fileCount.getLabel(), dataCount);
        JsonArray mTypes = new JsonArray();
        for (String mt : mimetypes) {
            mTypes.add(new JsonPrimitive(mt));
        }
        aggregation.add(JsonLDTerm.dcTerms("format").getLabel(), mTypes);
        aggregation.addProperty(JsonLDTerm.maxFileSize.getLabel(), maxFileSize);
        // Serialize oremap itself
        // FixMe - add missing hash values if needed and update context
        // (read and cache files or read twice?)
        createFileFromString("metadata/oai-ore.jsonld", oremapObject.toString());

        createFileFromString("metadata/datacite.xml", dataciteXml);

        // Add a bag-info file
        createFileFromString("bag-info.txt", generateInfoFile());

        logger.fine("Creating bag: " + bagName);

        ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(outputStream);

        /*
         * Add all the waiting contents - dirs created first, then data files are
         * retrieved via URLs in parallel (defaults to one thread per processor)
         * directly to the zip file
         */
        logger.fine("Starting write");
        writeTo(zipArchiveOutputStream);
        logger.fine("Zipfile Written");
        // Finish
        zipArchiveOutputStream.close();
        logger.fine("Closed");

        // Validate oremap - all entries are part of the collection
        for (int i = 0; i < resourceUsed.length; i++) {
            Boolean b = resourceUsed[i];
            if (b == null) {
                logger.warning("Problem: " + pidMap.get(resourceIndex.get(i)) + " was not used");
            } else if (!b) {
                logger.warning("Problem: " + pidMap.get(resourceIndex.get(i)) + " was not included successfully");
            } else {
                // Successfully included - now check for hash value and
                // generate if needed
                if (i > 0) { // Not root container
                    if (!checksumMap.containsKey(pidMap.get(resourceIndex.get(i)))) {

                        if (!childIsContainer(aggregates.get(i - 1).getAsJsonObject()))
                            logger.warning("Missing checksum hash for: " + resourceIndex.get(i));
                        // FixMe - actually generate it before adding the
                        // oremap
                        // to the zip
                    }
                }
            }

        }

        logger.info("Created bag: " + bagName);
        client.close();
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
                logger.fine("Writing to: " + bagFile.getAbsolutePath());
            }
            // Create an output stream backed by the file
            bagFileOS = new FileOutputStream(bagFile);
            if (generateBag(bagFileOS)) {
                //The generateBag call sets this.bagName to the correct value
                validateBagFile(bagFile);
                if (usetemp) {
                    logger.fine("Moving tmp zip");
                    origBagFile.delete();
                    bagFile.renameTo(origBagFile);
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Bag Exception: ", e);
            e.printStackTrace();
            logger.warning("Failure: Processing failure during Bagit file creation");
            return false;
        } finally {
            IOUtils.closeQuietly(bagFileOS);
        }
    }

    public void validateBag(String bagId) {
        logger.info("Validating Bag");
        ZipFile zf = null;
        InputStream is = null;
        try {
            File bagFile = getBagFile(bagId);
            zf = new ZipFile(bagFile);
            ZipArchiveEntry entry = zf.getEntry(getValidName(bagId) + "/manifest-sha1.txt");
            if (entry != null) {
                logger.info("SHA1 hashes used");
                hashtype = DataFile.ChecksumType.SHA1;
            } else {
                entry = zf.getEntry(getValidName(bagId) + "/manifest-sha512.txt");
                if (entry != null) {
                    logger.info("SHA512 hashes used");
                    hashtype = DataFile.ChecksumType.SHA512;
                } else {
                    entry = zf.getEntry(getValidName(bagId) + "/manifest-sha256.txt");
                    if (entry != null) {
                        logger.info("SHA256 hashes used");
                        hashtype = DataFile.ChecksumType.SHA256;
                    } else {
                        entry = zf.getEntry(getValidName(bagId) + "/manifest-md5.txt");
                        if (entry != null) {
                            logger.info("MD5 hashes used");
                            hashtype = DataFile.ChecksumType.MD5;
                        }
                    }
                }
            }
            if (entry == null)
                throw new IOException("No manifest file found");
            is = zf.getInputStream(entry);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine();
            while (line != null) {
                logger.fine("Hash entry: " + line);
                int breakIndex = line.indexOf(' ');
                String hash = line.substring(0, breakIndex);
                String path = line.substring(breakIndex + 1);
                logger.fine("Adding: " + path + " with hash: " + hash);
                checksumMap.put(path, hash);
                line = br.readLine();
            }
            IOUtils.closeQuietly(is);
            logger.info("HashMap Map contains: " + checksumMap.size() + " entries");
            checkFiles(checksumMap, bagFile);
        } catch (IOException io) {
            logger.log(Level.SEVERE,"Could not validate Hashes", io);
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Could not validate Hashes", e);
        } finally {
            IOUtils.closeQuietly(zf);
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
        logger.fine("BagPath: " + bagFile.getAbsolutePath());
        // Create an output stream backed by the file
        return bagFile;
    }

    private void validateBagFile(File bagFile) throws IOException {
        // Run a confirmation test - should verify all files and hashes
        
        // Check files calculates the hashes and file sizes and reports on
        // whether hashes are correct
        checkFiles(checksumMap, bagFile);

        logger.info("Data Count: " + dataCount);
        logger.info("Data Size: " + totalDataSize);
    }

    public static String getValidName(String bagName) {
        // Create known-good filename - no spaces, no file-system separators.
        return bagName.replaceAll("\\W", "-");
    }

    private void processContainer(JsonObject item, String currentPath) throws IOException {
        JsonArray children = getChildren(item);
        HashSet<String> titles = new HashSet<String>();
        String title = null;
        if (item.has(JsonLDTerm.dcTerms("Title").getLabel())) {
            title = item.get("Title").getAsString();
        } else if (item.has(JsonLDTerm.schemaOrg("name").getLabel())) {
            title = item.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString();
        }
        logger.fine("Adding " + title + "/ to path " + currentPath);
        currentPath = currentPath + title + "/";
        int containerIndex = -1;
        try {
            createDir(currentPath);
            // Add containers to pid map and mark as 'used', but no sha1 hash
            // value
            containerIndex = getUnusedIndexOf(item.get("@id").getAsString());
            resourceUsed[containerIndex] = true;
            pidMap.put(item.get("@id").getAsString(), currentPath);

        } catch (InterruptedException | IOException | ExecutionException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
            if (containerIndex != -1) {
                resourceUsed[containerIndex] = false;
            }
            throw new IOException("Unable to create bag");

        }
        for (int i = 0; i < children.size(); i++) {

            // Find the ith child in the overall array of aggregated
            // resources
            String childId = children.get(i).getAsString();
            logger.fine("Processing: " + childId);
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
                String dataUrl = child.get(JsonLDTerm.schemaOrg("sameAs").getLabel()).getAsString();
                logger.fine("File url: " + dataUrl);
                String childTitle = child.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString();
                if (titles.contains(childTitle)) {
                    logger.warning("**** Multiple items with the same title in: " + currentPath);
                    logger.warning("**** Will cause failure in hash and size validation in: " + bagID);
                } else {
                    titles.add(childTitle);
                }
                String childPath = currentPath + childTitle;
                JsonElement directoryLabel = child.get(JsonLDTerm.DVCore("directoryLabel").getLabel());
                if(directoryLabel!=null) {
                    childPath=currentPath + directoryLabel.getAsString() + "/" + childTitle;
                }
                

                String childHash = null;
                if (child.has(JsonLDTerm.checksum.getLabel())) {
                    ChecksumType childHashType = ChecksumType.fromString(
                            child.getAsJsonObject(JsonLDTerm.checksum.getLabel()).get("@type").getAsString());
                    if (hashtype == null) {
                    	//If one wasn't set as a default, pick up what the first child with one uses
                        hashtype = childHashType;
                    }
                    if (hashtype != null && !hashtype.equals(childHashType)) {
                        logger.warning("Multiple hash values in use - will calculate " + hashtype.toString()
                            + " hashes for " + childTitle);
                    } else {
                        childHash = child.getAsJsonObject(JsonLDTerm.checksum.getLabel()).get("@value").getAsString();
                        if (checksumMap.containsValue(childHash)) {
                            // Something else has this hash
                            logger.warning("Duplicate/Collision: " + child.get("@id").getAsString() + " has SHA1 Hash: "
                                + childHash + " in: " + bagID);
                        }
                        logger.fine("Adding " + childPath + " with hash " + childHash + " to checksumMap");
                        checksumMap.put(childPath, childHash);
                    }
                }
                if ((hashtype == null) | ignorehashes) {
                    // Pick sha512 when ignoring hashes or none exist
                    hashtype = DataFile.ChecksumType.SHA512;
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
                            logger.severe("Failed to read " + childPath);
                            throw e;
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                        if (childHash != null) {
                            JsonObject childHashObject = new JsonObject();
                            childHashObject.addProperty("@type", hashtype.toString());
                            childHashObject.addProperty("@value", childHash);
                            child.add(JsonLDTerm.checksum.getLabel(), (JsonElement) childHashObject);

                            checksumMap.put(childPath, childHash);
                        } else {
                            logger.warning("Unable to calculate a " + hashtype + " for " + dataUrl);
                        }
                    }
                    logger.fine("Requesting: " + childPath + " from " + dataUrl);
                    createFileFromURL(childPath, dataUrl);
                    dataCount++;
                    if (dataCount % 1000 == 0) {
                        logger.info("Retrieval in progress: " + dataCount + " files retrieved");
                    }
                    if (child.has(JsonLDTerm.filesize.getLabel())) {
                        Long size = child.get(JsonLDTerm.filesize.getLabel()).getAsLong();
                        totalDataSize += size;
                        if (size > maxFileSize) {
                            maxFileSize = size;
                        }
                    }
                    if (child.has(JsonLDTerm.schemaOrg("fileFormat").getLabel())) {
                        mimetypes.add(child.get(JsonLDTerm.schemaOrg("fileFormat").getLabel()).getAsString());
                    }

                } catch (Exception e) {
                    resourceUsed[index] = false;
                    e.printStackTrace();
                    throw new IOException("Unable to create bag");
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
            logger.severe("Reused ID: " + childId + " not found enough times in resource list");
        }
        return index;
    }

    private ArrayList<String> indexResources(String aggId, JsonArray aggregates) {

        ArrayList<String> l = new ArrayList<String>(aggregates.size() + 1);
        l.add(aggId);
        for (int i = 0; i < aggregates.size(); i++) {
            logger.fine("Indexing : " + i + " " + aggregates.get(i).getAsJsonObject().get("@id").getAsString());
            l.add(aggregates.get(i).getAsJsonObject().get("@id").getAsString());
        }
        logger.fine("Index created for " + aggregates.size() + " entries");
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

    private void checkFiles(HashMap<String, String> shaMap, File bagFile) {
        ExecutorService executor = Executors.newFixedThreadPool(numConnections);
        ZipFile zf = null;
        try {
            zf = new ZipFile(bagFile);

            BagValidationJob.setZipFile(zf);
            BagValidationJob.setBagGenerator(this);
            logger.fine("Validating hashes for zipped data files");
            int i = 0;
            for (Entry<String, String> entry : shaMap.entrySet()) {
                BagValidationJob vj = new BagValidationJob(bagName, entry.getValue(), entry.getKey());
                executor.execute(vj);
                i++;
                if (i % 1000 == 0) {
                    logger.info("Queuing Hash Validations: " + i);
                }
            }
            logger.fine("All Hash Validations Queued: " + i);

            executor.shutdown();
            try {
                while (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    logger.fine("Awaiting completion of hash calculations.");
                }
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Hash Calculations interrupted", e);
            } 
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } finally {
            IOUtils.closeQuietly(zf);
        }
        logger.fine("Hash Validations Completed");

    }

    public void addEntry(ZipArchiveEntry zipArchiveEntry, InputStreamSupplier streamSupplier) throws IOException {
        if (zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink())
            dirs.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(zipArchiveEntry, streamSupplier));
        else
            scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
    }

    public void writeTo(ZipArchiveOutputStream zipArchiveOutputStream)
            throws IOException, ExecutionException, InterruptedException {
        logger.fine("Writing dirs");
        dirs.writeTo(zipArchiveOutputStream);
        dirs.close();
        logger.fine("Dirs written");
        scatterZipCreator.writeTo(zipArchiveOutputStream);
        logger.fine("Files written");
    }

    static final String CRLF = "\r\n";

    private String generateInfoFile() {
        logger.fine("Generating info file");
        StringBuffer info = new StringBuffer();

        JsonArray contactsArray = new JsonArray();
        /* Contact, and it's subfields, are terms from citation.tsv whose mapping to a formal vocabulary and label in the oremap may change
         * so we need to find the labels used.
         */ 
        JsonLDTerm contactTerm = oremap.getContactTerm();
        if ((contactTerm != null) && aggregation.has(contactTerm.getLabel())) {

            JsonElement contacts = aggregation.get(contactTerm.getLabel());
            JsonLDTerm contactNameTerm = oremap.getContactNameTerm();
            JsonLDTerm contactEmailTerm = oremap.getContactEmailTerm();
            
            if (contacts.isJsonArray()) {
                for (int i = 0; i < contactsArray.size(); i++) {
                    info.append("Contact-Name: ");
                    JsonElement person = contactsArray.get(i);
                    if (person.isJsonPrimitive()) {
                        info.append(person.getAsString());
                        info.append(CRLF);

                    } else {
                        if(contactNameTerm != null) {
                          info.append(((JsonObject) person).get(contactNameTerm.getLabel()).getAsString());
                          info.append(CRLF);
                        }
                        if ((contactEmailTerm!=null) &&((JsonObject) person).has(contactEmailTerm.getLabel())) {
                            info.append("Contact-Email: ");
                            info.append(((JsonObject) person).get(contactEmailTerm.getLabel()).getAsString());
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
                    if(contactNameTerm != null) {
                      info.append(person.get(contactNameTerm.getLabel()).getAsString());
                      info.append(CRLF);
                    }
                    if ((contactEmailTerm!=null) && (person.has(contactEmailTerm.getLabel()))) {
                        info.append("Contact-Email: ");
                        info.append(person.get(contactEmailTerm.getLabel()).getAsString());
                        info.append(CRLF);
                    }
                }

            }
        } else {
            logger.warning("No contact info available for BagIt Info file");
        }

        info.append("Source-Organization: " + BundleUtil.getStringFromBundle("bagit.sourceOrganization"));
        // ToDo - make configurable
        info.append(CRLF);

        info.append("Organization-Address: " + WordUtils.wrap(
                BundleUtil.getStringFromBundle("bagit.sourceOrganizationAddress"), 78, CRLF + " ", true));
        info.append(CRLF);

        // Not a BagIt standard name
        info.append(
                "Organization-Email: " + BundleUtil.getStringFromBundle("bagit.sourceOrganizationEmail"));
        info.append(CRLF);

        info.append("External-Description: ");
        
        /* Description, and it's subfields, are terms from citation.tsv whose mapping to a formal vocabulary and label in the oremap may change
         * so we need to find the labels used.
         */
        JsonLDTerm descriptionTerm = oremap.getDescriptionTerm();
        JsonLDTerm descriptionTextTerm = oremap.getDescriptionTextTerm();
        if (descriptionTerm == null) {
            logger.warning("No description available for BagIt Info file");
        } else {
            info.append(
                    // FixMe - handle description having subfields better
                    WordUtils.wrap(getSingleValue(aggregation.get(descriptionTerm.getLabel()),
                            descriptionTextTerm.getLabel()), 78, CRLF + " ", true));

            info.append(CRLF);
        }
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
        String catalog = BundleUtil.getStringFromBundle("bagit.sourceOrganization") + " Catalog";
        if (aggregation.has(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel())) {
            catalog = aggregation.get(JsonLDTerm.schemaOrg("includedInDataCatalog").getLabel()).getAsString();
        }
        info.append(catalog + ":" + aggregation.get(JsonLDTerm.schemaOrg("name").getLabel()).getAsString());
        info.append(CRLF);

        return info.toString();

    }

    /**
     * Kludge - compound values (e.g. for descriptions) are sent as an array of
     * objects containing key/values whereas a single value is sent as one object.
     * For cases where multiple values are sent, create a concatenated string so
     * that information is not lost.
     * 
     * @param jsonElement
     *            - the root json object
     * @param key
     *            - the key to find a value(s) for
     * @return - a single string
     */
    String getSingleValue(JsonElement jsonElement, String key) {
        String val = "";
        if(jsonElement.isJsonObject()) {
            JsonObject jsonObject=jsonElement.getAsJsonObject();
            val = jsonObject.get(key).getAsString();
        } else if (jsonElement.isJsonArray()) {
            
            Iterator<JsonElement> iter = jsonElement.getAsJsonArray().iterator();
            ArrayList<String> stringArray = new ArrayList<String>();
            while (iter.hasNext()) {
                stringArray.add(iter.next().getAsJsonObject().getAsJsonPrimitive(key).getAsString());
            }
            if (stringArray.size() > 1) {
                val = String.join(",", stringArray);
            } else {
                val = stringArray.get(0);
            }
            logger.fine("Multiple values found for: " + key + ": " + val);
        }
        return val;
    }

    // Used in validation

    public void incrementTotalDataSize(long inc) {
        totalDataSize += inc;
    }

    public ChecksumType getHashtype() {
        return hashtype;
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
            logger.severe("Error finding children: " + o.toString());
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
                String urlString = url.toURL().toString();
                // Add key as param - check whether it is the only param or not
                urlString = urlString + ((urlString.indexOf('?') != -1) ? "&key=" : "?key=") + apiKey;
                request = new HttpGet(new URI(urlString));
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

    InputStreamSupplier getInputStreamSupplier(final String uriString) {

        return new InputStreamSupplier() {
            public InputStream get() {
                try {
                    URI uri = new URI(uriString);

                    int tries = 0;
                    while (tries < 5) {

                        logger.fine("Get # " + tries + " for " + uriString);
                        HttpGet getFile = createNewGetRequest(uri, null);
                        logger.finest("Retrieving " + tries + ": " + uriString);
                        CloseableHttpResponse response = null;
                        try {
                            response = client.execute(getFile);
                            // Note - if we ever need to pass an HttpClientContext, we need a new one per
                            // thread.
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode == 200) {
                                logger.finest("Retrieved: " + uri);
                                return response.getEntity().getContent();
                            }
                            logger.warning("Attempt: " + tries + " - Unexpected Status when retrieving " + uriString
                                    + " : " + statusCode);
                            if (statusCode < 500) {
                                logger.fine("Will not retry for 40x errors");
                                tries += 5;
                            } else {
                                tries++;
                            }
                            // Error handling
                            if (response != null) {
                                try {
                                    EntityUtils.consumeQuietly(response.getEntity());
                                    response.close();
                                } catch (IOException io) {
                                    logger.warning(
                                            "Exception closing response after status: " + statusCode + " on " + uri);
                                }
                            }
                        } catch (ClientProtocolException e) {
                            tries += 5;
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // Retry if this is a potentially temporary error such
                            // as a timeout
                            tries++;
                            logger.log(Level.WARNING, "Attempt# " + tries + " : Unable to retrieve file: " + uriString,
                                    e);
                            if (tries == 5) {
                                logger.severe("Final attempt failed for " + uriString);
                            }
                            e.printStackTrace();
                        }

                    }

                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                logger.severe("Could not read: " + uriString);
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

    public void setAuthenticationKey(String tokenString) {
        apiKey = tokenString;
    }

    public void setNumConnections(int numConnections) {
        this.numConnections = numConnections;
        logger.fine("BagGenerator will use " + numConnections + " threads");
    }

}
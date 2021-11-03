package no.unit.nva.s3;

import static no.unit.nva.s3.S3Driver.S3_SCHEME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import com.github.javafaker.Faker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3DriverTest {

    public static final int LARGE_NUMBER_OF_INPUTS = 10_000;
    public static final String EMPTY_STRING = "";
    public static final String ROOT = "/";
    private static final Faker FAKER = Faker.instance();
    private static final String SAMPLE_BUCKET = "sampleBucket";

    private static final String REMOTELY_EXISTING_BUCKET = "orestis-export";

    private static final String SOME_PATH = randomString();
    private S3Driver s3Driver;
    private S3Client s3Client;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
    }

    @Test
    @Tag("RemoteTest")
    public void listAllFilesReturnsListWithAllFilenamesInRemoteS3Folder() throws IOException {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        UnixPath expectedFilename = constructNestedPath();
        UnixPath parentFolder = expectedFilename.getParent().orElseThrow();
        String content = longText();
        s3Driver.insertFile(expectedFilename, content);
        List<UnixPath> files = s3Driver.listAllFiles(UnixPath.of(parentFolder.toString()));
        assertThat(files, CoreMatchers.hasItem(expectedFilename));
    }

    @Test
    @Tag("RemoteTest")
    public void listFilesReturnsPartialResultAndInformationForFetchingNextBatch() {

        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        UnixPath path = UnixPath.of("some/known/path");
        ListingResult result = s3Driver.listFiles(path, null, 100);
        List<UnixPath> totalResults = new ArrayList<>(result.getFiles());
        while (result.isTruncated()) {
            result = s3Driver.listFiles(path, result.getListingStartingPoint(), 100);
            totalResults.addAll(result.getFiles());
        }
        int knownSizeOfResult = 2000;
        assertThat(totalResults.size(), is(equalTo(knownSizeOfResult)));
    }

    @Test
    @Tag("RemoteTest")
    public void getUncompressedFileReturnsFileWhenFileExistsInRemoteFolder() throws IOException {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        UnixPath expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        String actualContent = s3Driver.getUncompressedFile(expectedFilePath).orElseThrow();
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    @Tag("RemoteTest")
    public void getFilesReturnsTheContentsOfAllFilesInARemoteFolder() throws IOException {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        UnixPath expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(1)));
        assertThat(actualContent, CoreMatchers.hasItem(expectedContent));
    }

    @Test
    public void listAllFilesReturnsListWithAllFilenamesInS3Folder() throws IOException {
        UnixPath firstPath = UnixPath.of(SOME_PATH).addChild(randomString()).addChild(randomString());
        UnixPath secondPath = UnixPath.of(SOME_PATH).addChild(randomString()).addChild(randomString());
        s3Driver.insertFile(firstPath, randomString());
        s3Driver.insertFile(secondPath, randomString());
        List<UnixPath> files = s3Driver.listAllFiles(UnixPath.of(SOME_PATH));
        assertThat(files, containsInAnyOrder(firstPath, secondPath));
    }

    @Test
    public void listFilesReturnsResultContainingPartialFileListAndNewListStartingPointAndSignForTerminatingListing()
        throws IOException {
        final UnixPath firstFilePath = UnixPath.of(SOME_PATH, "aaa");
        final UnixPath secondFilePath = UnixPath.of(SOME_PATH, "bbb");
        final String firstFileContent = randomString();
        final String secondFileContent = randomString();
        s3Driver.insertFile(firstFilePath, firstFileContent);
        s3Driver.insertFile(secondFilePath, secondFileContent);

        ListingResult firstBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), null, 1);
        assertThat(firstBatch.getListingStartingPoint(), is(equalTo(firstFilePath.toString())));
        assertThat(firstBatch.isTruncated(), is(true));

        ListingResult secondBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), firstFilePath.toString(), 1);
        assertThat(secondBatch.getListingStartingPoint(), is(equalTo(secondFilePath.toString())));
        assertThat(secondBatch.isTruncated(), is(false));
    }

    @Test
    public void getFilesReturnsTheContentsOfAllFilesInFolder() throws IOException {

        final UnixPath firstFilePath = UnixPath.of(SOME_PATH, randomString());
        final UnixPath secondFilePath = UnixPath.of(SOME_PATH, randomString());
        final String firstFileContent = randomString();
        final String secondFileContent = randomString();
        s3Driver.insertFile(firstFilePath, firstFileContent);
        s3Driver.insertFile(secondFilePath, secondFileContent);

        s3Driver = new S3Driver(s3Client, "ignored");
        List<String> actualContent = s3Driver.getFiles(UnixPath.of(SOME_PATH));

        assertThat(actualContent.size(), is(equalTo(2)));
        assertThat(actualContent, containsInAnyOrder(firstFileContent, secondFileContent));
    }

    @Test
    public void getFileReturnsFileWhenFileExists() throws IOException {
        UnixPath somePath = UnixPath.of(SOME_PATH);
        String expectedContent = randomString();
        URI fileLocation = s3Driver.insertFile(somePath, expectedContent);
        String actualContent = s3Driver.getUncompressedFile(toS3Path(fileLocation)).orElseThrow();
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    public void insertFileInsertsObjectEncodedInUtf8() throws IOException {
        UnixPath filePath = constructNestedPath();
        String expectedContent = randomString();
        s3Driver.insertFile(filePath, expectedContent);
        String actualContent = s3Driver.getFile(filePath);
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    public void getCompressedFileReturnsContentsWhenInputIsCompressed() throws IOException {
        String compressedFilename = "compressed.gz";
        String expectedContents = randomString();
        s3Driver.insertFile(UnixPath.of(compressedFilename), expectedContents);
        GZIPInputStream contents = s3Driver.getCompressedFile(UnixPath.of(compressedFilename));
        String result = readCompressedContents(contents);
        assertThat(result, is(equalTo(expectedContents)));
    }

    @Test
    public void insertFileWithInputStreamSendsDataToS3() throws IOException {
        String expectedContent = longText();
        InputStream inputStream = IoUtils.stringToStream(expectedContent);

        UnixPath somePath = UnixPath.of(randomString());
        s3Driver.insertFile(somePath, inputStream);
        String actualContent = s3Driver.getFile(somePath);
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    public void shouldCompressAndStoreFileWhenInputFilenameEndsWithGzAndContentIsString() throws IOException {
        String expectedContent = longText();
        UnixPath filePath = UnixPath.of("input.gz");
        s3Driver.insertFile(filePath, expectedContent);
        GZIPInputStream actualContentStream = s3Driver.getCompressedFile(filePath);
        String actualContent = readCompressedContents(actualContentStream);

        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    public void insertAndCompressFilesFilesStoresAllFilesCompressedInGzipStream() throws IOException {
        List<String> input = new ArrayList<>();
        for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
            input.add(longText());
        }
        UnixPath somePath = UnixPath.of(randomString());
        URI fileLocation = s3Driver.insertAndCompressObjects(somePath, input);
        GZIPInputStream compressedData = s3Driver.getCompressedFile(toS3Path(fileLocation));
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(compressedData));
        List<String> actualContent = inputReader.lines().collect(Collectors.toList());
        assertThat(actualContent, is(equalTo(input)));
    }

    @Test
    public void shouldReturnUriToS3FileLocationWhenSavingEvent() throws IOException {
        String content = randomString();
        UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
        URI fileLocation = s3Driver.insertEvent(someFolder, content);
        String randomFilename = new UriWrapper(fileLocation).getFilename();
        assertThat(fileLocation.getScheme(), is(equalTo(S3_SCHEME)));
        assertThat(fileLocation.getHost(), is(equalTo(SAMPLE_BUCKET)));
        assertThat(fileLocation.getPath(), is(equalTo("/parent/child1/child2/" + randomFilename)));
    }

    @Test
    public void shouldReadFileContentBasedOnUri() throws IOException {
        s3Driver = new S3Driver(new FakeS3Client(), "ignoredBucketName");
        String content = randomString();
        UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
        URI fileLocation = s3Driver.insertEvent(someFolder, content);
        String retrievedContent = s3Driver.readEvent(fileLocation);
        assertThat(retrievedContent, is(equalTo(content)));
    }

    @ParameterizedTest(name = "insertAndCompressFilesStoresAllFilesCompressedInGzipStreamInSpecifiedFolder")
    @ValueSource(strings = {EMPTY_STRING, ROOT})
    public void insertAndCompressFilesStoresAllFilesCompressedInGzipStreamInSpecifiedFolder(String pathPrefix)
        throws IOException {
        List<String> input = new ArrayList<>();
        for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
            input.add(longText());
        }
        String expectedFolderNeverContainsRootFolder = "parent/child/";
        URI fileLocation =
            s3Driver.insertAndCompressObjects(UnixPath.of(pathPrefix + expectedFolderNeverContainsRootFolder), input);
        GZIPInputStream compressedContent = s3Driver.getCompressedFile(toS3Path(fileLocation));
        List<String> actualContent = new BufferedReader(new InputStreamReader(compressedContent))
            .lines()
            .collect(Collectors.toList());
        assertThat(actualContent, is(equalTo(input)));
        assertThat(toS3Path(fileLocation).toString(), startsWith(expectedFolderNeverContainsRootFolder));
    }

    @Test
    public void insertAndCompressObjectsStoresAllFilesDirectlyUnderBucketWhenCalledWithoutPath() throws IOException {
        String input = longText();
        URI fileLocation = s3Driver.insertAndCompressObjects(List.of(input));
        String actualContent = s3Driver.getFile(toS3Path(fileLocation));
        assertThat(actualContent, is(equalTo(input)));
    }

    private static String randomFileName() {
        return FAKER.file().fileName();
    }

    private static String randomString() {
        return FAKER.lorem().word();
    }

    private static UnixPath constructNestedPath() {
        UnixPath expectedFileName = UnixPath.of(randomFileName());
        UnixPath parentFolder = UnixPath.of("some", "nested", "path");
        return parentFolder.addChild(expectedFileName);
    }

    private String readCompressedContents(GZIPInputStream contents) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(contents));
        return reader.lines().collect(Collectors.joining());
    }

    private UnixPath toS3Path(URI fileLocation) {
        return new UriWrapper(fileLocation).toS3bucketPath();
    }

    private String longText() {
        return FAKER.lorem().paragraph(10);
    }

    private S3Object sampleObjectListing(UnixPath firstExpectedObjectKey) {
        return S3Object.builder().key(firstExpectedObjectKey.toString()).build();
    }
}
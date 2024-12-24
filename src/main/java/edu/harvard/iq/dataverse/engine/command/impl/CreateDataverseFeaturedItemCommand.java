package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.api.dto.NewDataverseFeaturedItemDTO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

// TODO: Permissions
@RequiredPermissions({})
public class CreateDataverseFeaturedItemCommand extends AbstractCommand<DataverseFeaturedItem> {

    private final Dataverse dataverse;
    private final NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO;

    public CreateDataverseFeaturedItemCommand(DataverseRequest request, Dataverse dataverse, NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO) {
        super(request, dataverse);
        this.dataverse = dataverse;
        this.newDataverseFeaturedItemDTO = newDataverseFeaturedItemDTO;
    }

    @Override
    public DataverseFeaturedItem execute(CommandContext ctxt) throws CommandException {
        DataverseFeaturedItem featuredItem = new DataverseFeaturedItem();
        setImageIfAvailable(featuredItem);
        featuredItem.setContent(newDataverseFeaturedItemDTO.getContent());
        featuredItem.setDisplayOrder(newDataverseFeaturedItemDTO.getDisplayOrder());
        featuredItem.setDataverse(dataverse);
        return featuredItem;
    }

    private void setImageIfAvailable(DataverseFeaturedItem featuredItem) throws IllegalCommandException {
        if (newDataverseFeaturedItemDTO.getImageFileName() != null) {
            try {
                prepareUploadedImageFile();
            } catch (IOException e) {
                throw new RuntimeException(BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.imageFileProcessing", List.of(e.getMessage())), e);
            }
            featuredItem.setImageFileName(newDataverseFeaturedItemDTO.getImageFileName());
        }
    }

    private void prepareUploadedImageFile() throws IOException, IllegalCommandException {
        // Step 1: Create a temporary directory to store the uploaded image
        Path tempDir = createTempDir();
        File uploadedFile = new File(tempDir.toFile(), newDataverseFeaturedItemDTO.getImageFileName());

        if (!uploadedFile.exists()) {
            uploadedFile.createNewFile();
        }

        // Step 2: Convert the InputStream into a temporary file for validation
        File tempFile = FileUtil.inputStreamToFile(newDataverseFeaturedItemDTO.getFileInputStream());

        // Step 3: Validate the uploaded file (type and size)
        validateFile(tempFile);

        // Step 4: Copy the validated file to the final destination
        Files.copy(tempFile.toPath(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private Path createTempDir() throws IOException {
        Path tempRoot = Path.of(JvmSettings.DOCROOT_DIRECTORY.lookup(), JvmSettings.FEATURED_ITEMS_IMAGE_UPLOADS_DIRECTORY.lookup(), dataverse.getId().toString(), String.valueOf(newDataverseFeaturedItemDTO.getDisplayOrder()));
        Files.createDirectories(tempRoot);
        return tempRoot;
    }

    private void validateFile(File file) throws IOException, IllegalCommandException {
        validateFileType(file);
        validateFileSize(file);
    }

    private void validateFileType(File file) throws IOException, IllegalCommandException {
        Tika tika = new Tika();
        String mimeType = tika.detect(file);
        boolean isImageFile = mimeType != null && mimeType.startsWith("image/");
        if (!isImageFile) {
            throw new IllegalCommandException(
                    BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.invalidFileType"),
                    this
            );
        }
    }

    private void validateFileSize(File file) throws IllegalCommandException {
        Integer featuredItemsImageMaxSize = JvmSettings.FEATURED_ITEMS_IMAGE_MAXSIZE.lookup(Integer.class);
        if (file.length() > featuredItemsImageMaxSize) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.fileSizeExceedsLimit", List.of(featuredItemsImageMaxSize.toString())), this);
        }
    }
}

package edu.harvard.iq.dataverse.export.ddi;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class DdiExportUtil {

    private static final Logger logger = Logger.getLogger(DdiExportUtil.class.getCanonicalName());

    public static String datasetDtoAsJson2ddi(String datasetDtoAsJson) {
        logger.fine(JsonUtil.prettyPrint(datasetDtoAsJson));
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(datasetDtoAsJson, DatasetDTO.class);
        try {
            return dto2ddi(datasetDto);
        } catch (XMLStreamException ex) {
            Logger.getLogger(DdiExportUtil.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static String dto2ddi(DatasetDTO datasetDto) throws XMLStreamException {
        OutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);
        xmlw.writeStartElement("codeBook");
        xmlw.writeDefaultNamespace("http://www.icpsr.umich.edu/DDI");
        writeAttribute(xmlw, "version", "2.0");
        createStdyDscr(xmlw, datasetDto);
        createdataDscr(xmlw, datasetDto.getDatasetVersion().getFiles());
        xmlw.writeEndElement(); // codeBook
        xmlw.flush();
        String xml = outputStream.toString();
        return XmlPrinter.prettyPrintXml(xml);
    }

    /**
     * @todo This is just a stub, copied from DDIExportServiceBean. It should
     * produce valid DDI based on
     * http://guides.dataverse.org/en/latest/developers/tools.html#msv but it is
     * incomplete and will be worked on as part of
     * https://github.com/IQSS/dataverse/issues/2579 . We'll want to reference
     * the DVN 3.x code for creating a complete DDI.
     *
     * @todo Rename this from "study" to "dataset".
     */
    private static void createStdyDscr(XMLStreamWriter xmlw, DatasetDTO datasetDto) throws XMLStreamException {
        String title = dto2title(datasetDto.getDatasetVersion());
        String authors = dto2authors(datasetDto.getDatasetVersion());
        String persistentAgency = datasetDto.getProtocol();
        String persistentAuthority = datasetDto.getAuthority();
        String persistentId = datasetDto.getIdentifier();

        String citation = datasetDto.getDatasetVersion().getCitation();
        xmlw.writeStartElement("stdyDscr");
        xmlw.writeStartElement("citation");

        xmlw.writeStartElement("titlStmt");

        xmlw.writeStartElement("titl");
        xmlw.writeCharacters(title);
        xmlw.writeEndElement(); // titl

        xmlw.writeStartElement("IDNo");
        writeAttribute(xmlw, "agency", persistentAgency);
        xmlw.writeCharacters(persistentAuthority + "/" + persistentId);
        xmlw.writeEndElement(); // IDNo
        xmlw.writeEndElement(); // titlStmt

        xmlw.writeStartElement("rspStmt");

        xmlw.writeStartElement("AuthEnty");
        xmlw.writeCharacters(authors);
        xmlw.writeEndElement(); // AuthEnty

        xmlw.writeEndElement(); // rspStmt

        xmlw.writeStartElement("biblCit");
        xmlw.writeCharacters(citation);
        xmlw.writeEndElement(); // biblCit
        xmlw.writeEndElement(); // citation
        xmlw.writeEndElement(); // stdyDscr

    }

    /**
     * @todo Create a full dataDscr and otherMat sections of the DDI. This stub
     * adapted from the minimal DDIExportServiceBean example.
     */
    private static void createdataDscr(XMLStreamWriter xmlw, List<FileDTO> fileDtos) throws XMLStreamException {
        if (fileDtos.isEmpty()) {
            return;
        }
        xmlw.writeStartElement("dataDscr");
        xmlw.writeEndElement(); // dataDscr
        for (FileDTO fileDTo : fileDtos) {
            xmlw.writeStartElement("otherMat");
            writeAttribute(xmlw, "ID", "f" + fileDTo.getDatafile().getId());
            xmlw.writeStartElement("labl");
            xmlw.writeCharacters(fileDTo.getDatafile().getName());
            xmlw.writeEndElement(); // labl
            writeFileDescription(xmlw, fileDTo);
            xmlw.writeEndElement(); // otherMat
        }
    }

    private static void writeFileDescription(XMLStreamWriter xmlw, FileDTO fileDTo) throws XMLStreamException {
        xmlw.writeStartElement("txt");
        String description = fileDTo.getDatafile().getDescription();
        if (description != null) {
            xmlw.writeCharacters(description);
        }
        xmlw.writeEndElement(); // txt
    }

    private static String dto2title(DatasetVersionDTO datasetVersionDTO) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.title.equals(fieldDTO.getTypeName())) {
                        return fieldDTO.getSinglePrimitive();
                    }
                }
            }
        }
        return null;
    }

    private static String dto2authors(DatasetVersionDTO datasetVersionDTO) {
        for (Map.Entry<String, MetadataBlockDTO> entry : datasetVersionDTO.getMetadataBlocks().entrySet()) {
            String key = entry.getKey();
            MetadataBlockDTO value = entry.getValue();
            if ("citation".equals(key)) {
                for (FieldDTO fieldDTO : value.getFields()) {
                    if (DatasetFieldConstant.author.equals(fieldDTO.getTypeName())) {
                        for (HashSet<FieldDTO> foo : fieldDTO.getMultipleCompound()) {
                            for (Iterator<FieldDTO> iterator = foo.iterator(); iterator.hasNext();) {
                                FieldDTO next = iterator.next();
                                if (DatasetFieldConstant.authorName.equals(next.getTypeName())) {
                                    return next.getSinglePrimitive();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void writeAttribute(XMLStreamWriter xmlw, String name, String value) throws XMLStreamException {
        if (!StringUtilisEmpty(value)) {
            xmlw.writeAttribute(name, value);
        }
    }

    private static boolean StringUtilisEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    private static void saveJsonToDisk(String datasetVersionAsJson) throws IOException {
        Files.write(Paths.get("/tmp/out.json"), datasetVersionAsJson.getBytes());
    }

}

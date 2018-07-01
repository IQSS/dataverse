/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
public class DataCitation {

	private List<String> authors = new ArrayList<String>();
	private String title;
	private String fileTitle = null;
	private String year;
	private GlobalId persistentId;
	private String version;
	private String UNF = null;
	private String publisher;
	private boolean direct;

	private List<DatasetField> optionalValues = new ArrayList<>();
	private int optionalURLcount = 0;

	public DataCitation(DatasetVersion dsv) {
		this(dsv, false);
	}
	
	
	public DataCitation(DatasetVersion dsv, boolean direct) {
		this.direct = direct;
		// authors (or producer)
		getAuthorsFrom(dsv);

		// year
		year = getYearFrom(dsv);
		// title
		title = dsv.getTitle();

		// The Global Identifier:
		// It is always part of the citation for the local datasets;
		// And for *some* harvested datasets.
		persistentId = getPIDFrom(dsv, dsv.getDataset());

		// publisher
		publisher = getPublisherFrom(dsv);

		// version
		version = getVersionFrom(dsv);

		// UNF
		UNF = dsv.getUNF();

		// optional values
		for (DatasetFieldType dsfType : dsv.getDataset().getOwner().getCitationDatasetFieldTypes()) {
			DatasetField dsf = dsv.getDatasetField(dsfType);
			if (dsf != null) {
				optionalValues.add(dsf);

				if (dsf.getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.URL)) {
					optionalURLcount++;
				}
			}
		}
	}

	public DataCitation(FileMetadata fm) {
		this(fm, false);
	}
	
	public DataCitation(FileMetadata fm, boolean direct) {
		this.direct = direct;
		DatasetVersion dsv = fm.getDatasetVersion();

		// authors (or producer)
		getAuthorsFrom(dsv);

		// year
		year = getYearFrom(dsv);

		// file Title for direct File citation
		fileTitle = fm.getLabel();
		DataFile df = fm.getDataFile();
		// title
		title = dsv.getTitle();

		// The Global Identifier:
		// It is always part of the citation for the local datasets;
		// And for *some* harvested datasets.
		persistentId = getPIDFrom(dsv, df);

		// publisher
		publisher = getPublisherFrom(dsv);

		// version
		version = getVersionFrom(dsv);

		// UNF
		if (df.isTabularData() && df.getUnf() != null && !df.getUnf().isEmpty()) {
			UNF = df.getUnf();
		}

	}

	public String getAuthorsString() {
		return String.join(";", authors);
	}

	public String getTitle() {
		return title;
	}
	
	public String getFileTitle() {
		return fileTitle;
	}

	public boolean isDirect() {
		return direct;
	}

	
	public String getYear() {
		return year;
	}

	public GlobalId getPersistentId() {
		return persistentId;
	}

	public String getVersion() {
		return version;
	}

	public String getUNF() {
		return UNF;
	}

	public String getPublisher() {
		return publisher;
	}

	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean html) {
		// first add comma separated parts
		String separator = ". ";
		List<String> citationList = new ArrayList<>();
		citationList.add(formatString(getAuthorsString(), html));
		citationList.add(year);
		if ((fileTitle != null) && isDirect()) {
			citationList.add(formatString(fileTitle, html, "\""));
			citationList.add(formatString(title, html, "<i>", "</i>"));
		} else {
			citationList.add(formatString(title, html, "\""));
		}
		// QDRCustom: Use "Qualitative Data Repository" as distributor name
		citationList.add(formatString("Qualitative Data Repository", html));
		// QDRCustom: Show persistentID after distributor name
		if (persistentId != null) {
			citationList.add(formatURL(persistentId.toURL().toString(), persistentId.toURL().toString(), html)); // always
																													// show
																													// url
																													// format
		}
		citationList.add(formatString(publisher, html));
		citationList.add(version);

		StringBuilder citation = new StringBuilder(citationList.stream().filter(value -> !StringUtils.isEmpty(value))
				// QDRCustom: Use period to join values, not comma
				.collect(Collectors.joining(separator)));

		if ((fileTitle != null) && !isDirect()) {
			citation.append("; " + formatString(fileTitle, html, "") + " [fileName]");
		}
		// append UNF
		if (!StringUtils.isEmpty(UNF)) {
			// QDRCustom: Use period to join values, not comma
			citation.append(". ").append(UNF).append(" [fileUNF]");
		}

		for (DatasetField dsf : optionalValues) {
			String displayName = dsf.getDatasetFieldType().getDisplayName();
			String displayValue;

			if (dsf.getDatasetFieldType().getFieldType().equals(DatasetFieldType.FieldType.URL)) {
				displayValue = formatURL(dsf.getDisplayValue(), dsf.getDisplayValue(), html);
				if (optionalURLcount == 1) {
					displayName = "URL";
				}
			} else {
				displayValue = formatString(dsf.getDisplayValue(), html);
			}

			citation.append(" [").append(displayName).append(": ").append(displayValue).append("]");
		}

		return citation.toString();
	}

	public void writeBibtex(OutputStream os) throws IOException {
		//Use UTF-8?
		 Writer out
		   = new BufferedWriter(new OutputStreamWriter(os));
		if(getFileTitle() !=null && isDirect()) {
			out.write("@incollection{");
		} else {
			out.write("@data{");
		}
		out.write(persistentId.getIdentifier() + "_" + year + "," + "\r\n");
		out.write("author = {");
		out.write(String.join(" and ", authors));
		out.write("},\r\n");
		out.write("publisher = {");
		out.write(publisher);
		out.write("},\r\n");
		if(getFileTitle() !=null && isDirect()) {
		out.write("title = {");
		out.write(fileTitle);
		out.write("},\r\n");
		out.write("booktitle = {");
		out.write(title);
		out.write("},\r\n");
		} else {
			out.write("title = {");
			out.write(title);
			out.write("},\r\n");
			
		}
		out.write("year = {");
		out.write(year);
		out.write("},\r\n");
		out.write("doi = {");
		out.write(persistentId.getAuthority());
		out.write("/");
		out.write(persistentId.getIdentifier());
		out.write("},\r\n");
		out.write("url = {");
		out.write(persistentId.toURL().toString());
		out.write("}\r\n");
		if(getFileTitle()!=null) {
			if(isDirect()) {
				out.write("note = {");
				out.write("This reference is to a file ");
						if(getUNF()!=null) {
							out.write("(UNF=" + getUNF()+")");
						}
						out.write(", with the given doi, within a dataset");
				out.write("}\r\n");
			} else {
				out.write("note = {");
				out.write("This reference is to a file ");
				if(getUNF()!=null) {
					out.write("(UNF=" + getUNF()+")");
				}
				out.write(" within a dataset with the given doi");
				out.write("}\r\n");
			}
			out.write("}");

		}
		out.flush();
	}

	public void writeRIS(OutputStream os) throws IOException {
		//Use UTF-8?
		Writer out
		   = new BufferedWriter(new OutputStreamWriter(os));
		out.write("Provider: " + publisher + "\r\n");
		out.write("Content: text/plain; charset=\"us-ascii\"" + "\r\n");
		// Using type "DBASE" - "Online Database", for consistency with
		// EndNote (see the longer comment in the EndNote section below)>

		
		if (getFileTitle()!=null) {
			out.write("TY  - CHAP" + "\r\n");
			out.write("T1  - " + getFileTitle() + "\r\n");
			out.write("T2  - " + getTitle() + "\r\n");
		} else {
			out.write("TY  - DBASE" + "\r\n");
			out.write("T1  - " + getTitle() + "\r\n");
		}
		for (String author : authors) {
			out.write("AU  - " + author + "\r\n");
		}
		out.write("DO  - " + persistentId.toString() + "\r\n");
		out.write("PY  - " + year + "\r\n");
		out.write("UR  - " + persistentId.toURL().toString() + "\r\n");
		out.write("PB  - " + publisher + "\r\n");

		// a DataFile citation also includes filename und UNF, if applicable:
		if (getFileTitle() != null) {
			if(!isDirect()) {
				out.write("C1  - " + getFileTitle() + "\r\n");
			}
			if (getUNF() != null) {
				out.write("C2  - " + getUNF() + "\r\n");
			}
			if(isDirect()) {
			out.write("N1  - This reference is to a file, with the given identifier, within a dataset.\r\n");
			} else {
				out.write("N1  - This reference is to a file within the dataset with the given identifier.\r\n");
			}
		} else {
			out.write("N1  - This reference is to a dataset.\r\n");
		}

		// closing element:
		out.write("ER  - \r\n");
		out.flush();
	}



	// helper methods
	private String formatString(String value, boolean escapeHtml) {
		return formatString(value, escapeHtml, "");
	}

	private String formatString(String value, boolean escapeHtml, String wrapperFront) {
		return formatString(value, escapeHtml, wrapperFront, wrapperFront);
	}

	private String formatString(String value, boolean escapeHtml, String wrapperStart, String wrapperEnd) {
		if (!StringUtils.isEmpty(value)) {
			return new StringBuilder(wrapperStart).append(escapeHtml ? StringEscapeUtils.escapeHtml(value) : value)
					.append(wrapperEnd).toString();
		}
		return null;
	}

	private String formatURL(String text, String url, boolean html) {
		if (text == null) {
			return null;
		}

		if (html && url != null) {
			return "<a href=\"" + url + "\" target=\"_blank\">" + StringEscapeUtils.escapeHtml(text) + "</a>";
		} else {
			return text;
		}

	}

	private String getYearFrom(DatasetVersion dsv) {
		String year = "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
		if (!dsv.getDataset().isHarvested()) {
			Date citationDate = dsv.getCitationDate();
			if (citationDate == null) {
				if (dsv.getDataset().getPublicationDate() != null) {
					citationDate = dsv.getDataset().getPublicationDate();
				} else { // for drafts
					citationDate = new Date();
				}
			}

			year = new SimpleDateFormat("yyyy").format(citationDate);

		} else {
			try {
				year = sdf.format(sdf.parse(dsv.getDistributionDate()));
			} catch (ParseException ex) {
				// ignore
			} catch (Exception ex) {
				// ignore
			}
		}
		return year;
	}

	private void getAuthorsFrom(DatasetVersion dsv) {

		dsv.getDatasetAuthors().stream().forEach((author) -> {
			if (!author.isEmpty()) {
				String an = author.getName().getDisplayValue().trim();
				authors.add(an);
			}
		});
		if (authors.size() == 0) {
			authors = dsv.getDatasetProducerNames();
		}
	}

	private String getPublisherFrom(DatasetVersion dsv) {
		if (!dsv.getDataset().isHarvested()) {
			return dsv.getRootDataverseNameforCitation();
		} else {
			return dsv.getDistributorName();
			// remove += [distributor] SEK 8-18-2016
		}

	}

	private String getVersionFrom(DatasetVersion dsv) {
		String version = "";
		if (!dsv.getDataset().isHarvested()) {
			if (dsv.isDraft()) {
				version = "DRAFT VERSION";
			} else if (dsv.getVersionNumber() != null) {
				version = "V" + dsv.getVersionNumber();
				if (dsv.isDeaccessioned()) {
					version += ", DEACCESSIONED VERSION";
				}
			}
		}
		return version;
	}

	private GlobalId getPIDFrom(DatasetVersion dsv, DvObject dv) {
		if (!dsv.getDataset().isHarvested()
				|| HarvestingClient.HARVEST_STYLE_VDC.equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())
				|| HarvestingClient.HARVEST_STYLE_ICPSR.equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())
				|| HarvestingClient.HARVEST_STYLE_DATAVERSE
						.equals(dsv.getDataset().getHarvestedFrom().getHarvestStyle())) {
			if (!StringUtils.isEmpty(dv.getIdentifier())) {
				// creating a global id like this:
				// persistentId = new GlobalId(dsv.getDataset().getGlobalId());
				// you end up doing new GlobalId((New GlobalId(dsv.getDataset())).toString())
				// - doing an extra formatting-and-parsing-again
				// This achieves the same thing:
				return new GlobalId(dsv.getDataset());
			}
		}
		return null;
	}
}

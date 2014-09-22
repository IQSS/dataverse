package edu.harvard.iq.dataverse.util;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *  Convenience class to create a zip file, used by ShapefileHandler
 *
 * source: http://www.avajava.com/tutorials/lessons/how-can-i-create-a-zip-file-from-a-set-of-files.html
 * 
 * 
 * 
 */
public class ZipMaker{

    private static boolean DEBUG = false;
    
    public static void main(String[] args){
        
    }

    public ZipMaker(List<String> filenames, String inputDirname, String outputZipFilename){

        try {
			FileOutputStream fos = new FileOutputStream(outputZipFilename);
			ZipOutputStream zip_output_stream = new ZipOutputStream(fos);

            for(String fname: filenames){
            
                String fullpath = new String(inputDirname + '/' + fname);
                addToZipFile(fname, fullpath, zip_output_stream);
    			
            }

			zip_output_stream.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }


public static void addToZipFile(String fileName, String fullFilepath, ZipOutputStream zip_output_stream) throws FileNotFoundException, IOException {

        if (DEBUG){
		    System.out.println("Writing '" + fileName + "' to zip file");
        }
        
		File file = new File(fullFilepath);
		FileInputStream file_input_stream = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zip_output_stream.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = file_input_stream.read(bytes)) >= 0) {
			zip_output_stream.write(bytes, 0, length);
		}

		zip_output_stream.closeEntry();
		file_input_stream.close();
	}

}

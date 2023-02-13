/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.util;

/*
 import com.eim.exceptions.EIMCouldNotWriteToFileException;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import sun.misc.BASE64Decoder;
 import sun.misc.BASE64Encoder;
 */
/**
 * EIMFileUtils
 *
 * @author Denis Meyer
 */
public class EIMFileUtils {

    /*
     private static final Logger logger = LogManager.getLogger(EIMFileUtils.class.getName());
     private static EIMFileUtils instance = null;

     protected EIMFileUtils() {
     if(logger.isDebugEnabled()) {
     logger.debug("Initializing EIMFileUtils");
     }
     }

     public static EIMFileUtils getInstance() {
     if (instance == null) {
     instance = new EIMFileUtils();
     }
     return instance;
     }

     public boolean fileExists(String fileName) {
     return new File(fileName).exists();
     }

     public String getName(String fileName) {
     return new File(fileName).getName();
     }

     public boolean deleteFile(String fileName) {
     File f = new File(fileName);
     return !f.exists() || (f.exists() && f.delete());
     }

     public void writeToFile(String fileName, String content) throws IOException, EIMCouldNotWriteToFileException {
     OutputStreamWriter writer = null;
     try {
     File f = new File(fileName);

     if (!f.exists()) {
     if (!f.createNewFile()) {
     throw new EIMCouldNotWriteToFileException("Could not write to file: " + fileName);
     }
     }
     if (f.canWrite()) {
     writer = new OutputStreamWriter(
     new FileOutputStream(f),
     EIMConstants.CHARSET
     );
     writer.write(content);
     }
     } catch (IOException e) {
     throw e;
     } finally {
     try {
     if (writer != null) {
     writer.close();
     }
     } catch (IOException e) {
     throw e;
     }
     }
     }

     public String readFromFile(String fileName) throws IOException, EIMCouldNotWriteToFileException {
     BufferedReader reader = null;
     InputStreamReader in = null;
     String cont = "";
     try {
     File f = new File(fileName);

     if (!f.exists() || !f.canRead()) {
     throw new EIMCouldNotWriteToFileException("Could not read from file: " + fileName);
     }

     in = new InputStreamReader(
     new FileInputStream(f),
     EIMConstants.CHARSET
     );

     reader = new BufferedReader(in);
     String c_line = reader.readLine();
     while (c_line != null) {
     if (!cont.isEmpty()) {
     cont += EIMConstants.LINESEPARATOR;
     }
     cont += c_line.trim();
     c_line = reader.readLine();
     }
     } catch (IOException e) {
     throw e;
     } finally {
     try {
     if (in != null) {
     in.close();
     }
     if (reader != null) {
     reader.close();
     }
     } catch (IOException e) {
     throw e;
     }
     }

     return cont.trim();
     }

     public void writeToBinaryFile(String fileName, byte[] toWrite)
     throws FileNotFoundException, IOException, EIMCouldNotWriteToFileException {
     BASE64Encoder base64Encoder = new BASE64Encoder();
     writeToFile(fileName, base64Encoder.encodeBuffer(toWrite));
     }

     public byte[] readFromBinaryFile(String fileName) throws FileNotFoundException,
     IOException,
     EIMCouldNotWriteToFileException {
     BASE64Decoder base64Decoder = new BASE64Decoder();
     return base64Decoder.decodeBuffer(readFromFile(fileName));
     }
     */
}

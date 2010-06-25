package com.bmape.applostat.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * 
 * @author martinoswald
 */
public class Utils {
	
	private Utils() {
	}
	
	public static void writePropertiesFile(Properties properties, File file, String comments) {
		
		OutputStream outStream = null;	
		try {
			try {
				outStream = new BufferedOutputStream(new FileOutputStream(file));	
				properties.store(outStream, comments);

			} finally {
				if (outStream != null) {
					outStream.close();
				}
			}
		} catch(IOException io) {
			System.out.println("Could not write properties file.");
		}
	}
	
	public static Properties readPropertiesFile(File file) {
		
		Properties properties = new Properties();
		InputStream inStream = null;	
		try {
			try {
				inStream = new BufferedInputStream(new FileInputStream(file));	
				properties.load(inStream);
			} finally {
				if (inStream != null) {
					inStream.close();
				}
			}
		} catch(IOException io) {
			System.out.println("Could not read properties file.");
		} 
		
		return properties;
	}
	
}

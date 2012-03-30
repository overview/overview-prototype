package snappy.ui;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import au.com.bytecode.opencsv.CSVWriter;

public class InteractionLogger {
	   private static InteractionLogger instance = null;
	   
	   private static CSVWriter csv=null;
	   private static DateFormat df=null;
	   
	   protected InteractionLogger() {
	      // Exists only to defeat instantiation.
	   }
	   
	   public static void openLog(String filename) {
		   FileWriter file=null;
		   try {
			   file = new FileWriter(filename, true);		// true == append
		   } catch (IOException io) {
			   System.out.println("Could not open log file " + filename);
		   }
		   csv = new CSVWriter(file);
	       df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
	   }
	   
	   public static void log(String type, String params) {
		   if (csv != null) {
			   Date date = new Date();
			   String [] line = {df.format(date), type, params};
			   csv.writeNext(line);
		   }
	   }
	   
	   // empty params version
	   public static void log(String type) {
		   log(type,"");
	   }

	   public static void closeLog() {
		   try {
			   csv.close();
		   } catch (IOException io) {
		   }
	   }
	}
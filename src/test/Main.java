package test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import parser.Parser;
import java.util.Properties;

public class Main {
	
	private static final Map<String, DataDependenceOptions> dataSetup;
	private static final Map<String, ControlDependenceOptions> controlSetup;
	static {
		Map<String, DataDependenceOptions> dMap = new HashMap<>();
		dMap.put("full", DataDependenceOptions.FULL);
		dMap.put("no_base_ptrs", DataDependenceOptions.NO_BASE_PTRS);
		dMap.put("no_base_no_heap", DataDependenceOptions.NO_BASE_NO_HEAP);
		dMap.put("no_heap", DataDependenceOptions.NO_HEAP);
		dMap.put("none", DataDependenceOptions.NONE);
		dMap.put("reflection", DataDependenceOptions.REFLECTION);
		dMap.put("no_base_no_exceptions", DataDependenceOptions.NO_BASE_NO_EXCEPTIONS);
		dMap.put("no_base_no_heap_no_exceptions", DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS);
		dMap.put("no_exceptions", DataDependenceOptions.NO_EXCEPTIONS);
		dMap.put("no_heap_no_exceptions", DataDependenceOptions.NO_HEAP_NO_EXCEPTIONS);
		dataSetup = Collections.unmodifiableMap(dMap);

		Map<String, ControlDependenceOptions> cMap = new HashMap<>();
		cMap.put("full", ControlDependenceOptions.FULL);
		cMap.put("none",ControlDependenceOptions.NONE);
		cMap.put("no_exceptional_edges", ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		controlSetup = Collections.unmodifiableMap(cMap);
	}
	
	public static boolean isNullOrEmpty( final Collection< ? > c ) {
	    return c == null || c.isEmpty();
	}
	
	public static void main(String[] arg){
		try {
			
			CustomSlicer slicer = new CustomSlicer();
			
			// PARAMETER
			Properties prop = new Properties();
			InputStream input = null;
			String mode = "";
			String appJar = "";
			String mainClass = ""; 
			String srcCaller = "";
			String srcCallee = ""; 
			// we only want backwards
			boolean goBackward = true; 
			String javaSourcefolder = "";
			String outputdir = "";
			boolean multiple_caller = false;
			boolean split_java_output = false;
			String srcCalleeClass = null;
			boolean only_public_entry = false;
			DataDependenceOptions dOptions = DataDependenceOptions.FULL;		
			ControlDependenceOptions cOptions = ControlDependenceOptions.FULL;
		
			try {
				input = new FileInputStream("slicer.conf");

				// load a properties file
				prop.load(input);

				// get the property value and print it out
				Collection<Object> prop_values = prop.values(); 
				if (isNullOrEmpty(prop_values)){
					System.out.println("Missing values in slicer.conf");
					return;
				}
				mode = prop.getProperty("mode");
				appJar = prop.getProperty("jar");
				mainClass = prop.getProperty("mainclass");
				srcCaller = prop.getProperty("src_caller");
				srcCallee = prop.getProperty("src_callee");
				srcCalleeClass = prop.getProperty("src_callee_class", "");
				//goBackward = Boolean.parseBoolean(prop.getProperty("gobackwards", "true"));
				javaSourcefolder = prop.getProperty("java_source_folder");
				outputdir = prop.getProperty("output_dir", "output");
				split_java_output = Boolean.parseBoolean(prop.getProperty("split_java_output", "false"));
				multiple_caller = Boolean.parseBoolean(prop.getProperty("multiple_caller", "false"));
				only_public_entry = Boolean.parseBoolean(prop.getProperty("only_public_entry", "true"));
				try{
					if (!dataSetup.containsKey(prop.getProperty("datadep_opts")) || !controlSetup.containsKey(prop.getProperty("controldep_opts"))){
						System.err.println("Either datadep_opts or controldep_opts is not set correct.\nCheck for typos and try again.");
						return;
					}
					dOptions = dataSetup.get(prop.getProperty("datadep_opts"));
					cOptions = controlSetup.get(prop.getProperty("controldep_opts"));
				} catch (IllegalArgumentException e) {
					System.err.println(e);
					return;
				}

			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		
		    // CALL SLICER
			System.out.println("Calling Slicer with: \n"+ appJar + " " + mainClass + " " + srcCaller + " " + srcCallee + " " + srcCalleeClass + " " + dOptions + " " + cOptions);
			
			slicer.doSlicing(appJar, mainClass, only_public_entry, srcCaller, srcCallee, srcCalleeClass, javaSourcefolder, outputdir, split_java_output, multiple_caller, dOptions, cOptions, prop);
			System.out.println("\n============\nSlicing done.");
			return;
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}

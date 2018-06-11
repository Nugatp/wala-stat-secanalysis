package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class ConfigurationHelper {

	private static String[] controlDependecies = {"full", "none",  "no_exceptional_edges"};
	private static String[] dataDependencies = {"full","no_base_ptrs", "no_base_no_heap", 
			"no_heap", "none", "reflection", "no_exceptions", "no_base_no_exceptions", "no_heap_no_exceptions", 
			"no_base_no_heap_no_exceptions"};
	private static String NEW_LINE = "\n";
	private String conf;
	
	public String getConf(){
		return conf;
	}

	public void extendConfiguration(final String seed){
		BufferedReader br;
		try {
			br = new  BufferedReader(new FileReader(seed));					
			String line = null;
			StringBuilder resultBuilder = new StringBuilder();
			while((line = br.readLine()) != null){
				String[] params = line.split(" ");
				if(params.length == 6){
					StringBuilder builder = new StringBuilder();
					for(int i=0; i<5; i++){
						builder.append(params[i] + " ");
					}					
					String partialResult = builder.toString();
					for(String controlDependency: controlDependecies){
						for(String dataDepLine: dataDependencies){
							String newLine = partialResult + dataDepLine + " " +controlDependency  + " ";
							newLine = newLine + params[5];
							resultBuilder.append(newLine);
							resultBuilder.append(NEW_LINE);
						}		
					}
				}
			}
			conf = resultBuilder.toString();
		}catch(IOException e){
			System.err.println("Seed nicht gefunden.");
		}
	}
	
	public String writeToFile(){
		Calendar calendar = Calendar.getInstance();
		String fileName = calendar.getTime().toString().replace(" ", "_") + ".config";
		fileName = fileName.replace(":", "-");
		Path file = Paths.get(fileName);
		List<String> lines = Arrays.asList(getConf());
		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
			return fileName;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
		}
		return null;
	}
	
	public static void main(String[] argsv){
		ConfigurationHelper ch = new ConfigurationHelper();
		ch.extendConfiguration("/home/mathias/workspace/Evaluation/seed");
		ch.writeToFile();
	}
}

package logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.Parser;
import test.SliceMapper;

public class LogBuilder {

	private SliceMapper sm;
	private Parser parser;

	private static String EMPTY_STRING = "";	
	private static String SEPERATOR = "\t**********************************\t\n";
	private static String SEPERATOR_2 = "--------------------------------------------------------------------------------------\n";
	private static String SLICE_HEADLINE = "rekonstruierter-slice: \n";
	private static String CALLER = "Caller: ";
	private static String CALLEE = "Callee: ";
	private static String DDO = "DataDependency-Option: ";
	private static String CDO = "ControlDependency-Option: ";
	private static String JAVA_FILE = "JavaFile: ";
	private static String ELAPSED_TIME_0 = "CallGraph Aufbauzeit: ";
	private static String ELAPSED_TIME_1 = "benötigte Slice-Zeit: ";
	private static String ACTUAL_SLICE_SIZE = " rekonstruierbare Zeilen"; 
	private static String NEW_LINE = "\n";
	private static String TAB = "\t";
	private static String COL = "|";
	private static int CONF_LINE_LENGTH = 8;

	Map<Row, String> log;

	public LogBuilder(final Set<Row> confRows){
		sm = new SliceMapper();
		log = new HashMap<>();
		List<Row> sortedConfRows = new ArrayList<>();
		sortedConfRows.addAll(confRows);
		Collections.sort(sortedConfRows);
		for(Row confRow: sortedConfRows){
			log.put(confRow, EMPTY_STRING);
		}
	}

	public void buildCallGraphSize(final Row confLineNumber, final String info){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(info);
			builder.append(NEW_LINE);
			log.put(confLineNumber, builder.toString());
		}
	}
	
	public void buildTabHeadLine(final Row confLineNumber, final String confLine){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null && confLine.split(" ").length == CONF_LINE_LENGTH){
			String[] params = confLine.split(" ");			
			builder.append(log.get(confLineNumber));
			builder.append(NEW_LINE);
			builder.append(SEPERATOR_2);
			builder.append(params[7]);
			builder.append(NEW_LINE);
			builder.append(SEPERATOR_2);
			builder.append(NEW_LINE);
			builder.append(CALLER);
			builder.append(TAB);
			builder.append(COL);
			builder.append(CALLEE);
			builder.append(TAB);
			builder.append(COL);
			builder.append(DDO);
			builder.append(TAB);
			builder.append(COL);
			builder.append(CDO);
			builder.append(TAB);
			builder.append(COL);
			builder.append(ELAPSED_TIME_0);
			builder.append(TAB);
			builder.append(COL);
			builder.append(ELAPSED_TIME_1);
			builder.append(TAB);
			builder.append(COL);
			builder.append(ELAPSED_TIME_0);
			builder.append(TAB);
			builder.append(COL);
			builder.append("");
			builder.append(TAB);
			builder.append(COL);
			builder.append("");
			builder.append(TAB);
			builder.append(COL);
			builder.append(ACTUAL_SLICE_SIZE);
			builder.append(TAB);
			builder.append(COL);
			builder.append(NEW_LINE);
			builder.append(params[2]);
			builder.append(TAB);
			builder.append(COL);
			builder.append(params[3]);
			builder.append(TAB);
			builder.append(COL);
			builder.append(params[4]);
			builder.append(TAB);
			builder.append(COL);
			builder.append(params[6]);
			builder.append(TAB);
			builder.append(COL);
		}
		log.put(confLineNumber, builder.toString());
	}
	
	public void buildInfoTab(final Row confLineNumber, final String info){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(info);
			builder.append(TAB);
			builder.append(COL);
		}
		log.put(confLineNumber, builder.toString());
	}
	
	
	public void buildRow(final Row confLineNumber){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(NEW_LINE);
			builder.append(SEPERATOR_2);
			builder.append(NEW_LINE);
		}
		log.put(confLineNumber, builder.toString());
	}
	
	public void buildSDGSize(final Row confLineNumber, final String info){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(info);
			builder.append(NEW_LINE);
			log.put(confLineNumber, builder.toString());
		}
	}

	public void buildSliceSize(final Row confLineNumber, final String info){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(info);
			builder.append(NEW_LINE);
			log.put(confLineNumber, builder.toString());
		}
	}
	
	public void buildActualSliceSize(final Row confLineNumber, final List<Integer> lineNumbers){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(lineNumbers.size());
			builder.append(ACTUAL_SLICE_SIZE);
			builder.append(NEW_LINE);
			builder.append(SEPERATOR_2);
			log.put(confLineNumber, builder.toString());
		}
	}

	public void buildElapsedTime(final Row confLineNumber, final Map<Integer, String> info){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(ELAPSED_TIME_0);
			builder.append(info.get(0));
			builder.append(NEW_LINE);
			builder.append(ELAPSED_TIME_1);
			builder.append(info.get(1));
			builder.append(NEW_LINE);
			log.put(confLineNumber, builder.toString());	
		}
	}

	public void buildHeadLine(final Row confLineNumber, final String confLine){
		StringBuilder builder = new StringBuilder();
		String[] params = confLine.split(" ");
		if(params.length == CONF_LINE_LENGTH){
			builder.append(SEPERATOR_2);
			builder.append(JAVA_FILE);
			builder.append(params[7]);
			builder.append(NEW_LINE);
			builder.append(CALLER);
			builder.append(params[2]);
			builder.append(NEW_LINE);
			builder.append(CALLEE);
			builder.append(params[3]);
			builder.append(NEW_LINE);
			builder.append(DDO);
			builder.append(params[5]);
			builder.append(NEW_LINE);
			builder.append(CDO);
			builder.append(params[6]);
			builder.append(NEW_LINE);
			builder.append(SEPERATOR);
		}
		log.put(confLineNumber, builder.toString());
	}
	
	public void buildOriginal(final Map<String, Set<Integer>> lineNumbers, final Row confLineNumber){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append("Original\n");
			for(Map.Entry<String, Set<Integer>> elem: lineNumbers.entrySet()){
				if(sm.markLinesOfCode(elem.getKey(), elem.getValue()) != null)
					builder.append(sm.markLinesOfCode(elem.getKey(), elem.getValue()));
			}
			builder.append(SEPERATOR_2);
			log.put(confLineNumber, builder.toString());
		}
	}	
	

	public void buildLogForSlice(final String javaFileName, final Map<String, Set<Integer>> lineNumbers, final Row confLineNumber){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append(SLICE_HEADLINE);
			builder.append("main-class: " + javaFileName + "\n");

			try{
				for(Map.Entry<String, Set<Integer>> elem: lineNumbers.entrySet()){
					builder.append(sm.getLinesOfCode(elem.getKey(), 
							parser.getModifiedSlice(elem.getKey(), elem.getValue())));
				}
			}catch(Exception e){
				System.out.println("Programmausschnitt konnte nicht mit JavaParser rekonstruiert werden");
			}
			log.put(confLineNumber, builder.toString());
		}
	}
	
	public void buildSliceLines(final Row confLineNumber, final Map<String, Set<Integer>> lineNumbers){
		StringBuilder builder = new StringBuilder();
		if(log.get(confLineNumber) != null){
			builder.append(log.get(confLineNumber));
			builder.append("rekonstruierbare Zeilen:\n");
			for(Map.Entry<String, Set<Integer>> elem: lineNumbers.entrySet()){
				List<Integer> lines =  new ArrayList<>();
				lines.addAll(elem.getValue());
				Collections.sort(lines);
				builder.append(elem.getKey() +": " + lines.toString() + NEW_LINE);
			}
			builder.append(SEPERATOR_2);
			log.put(confLineNumber, builder.toString());
		}
	}
	
	
	public void buildOverview(final String javaFileName){
		StringBuilder builder = new StringBuilder();
		builder.append(sm.getLinesOfCode(javaFileName, new HashSet<Integer>()));
		Row row = new Row(-1);
		log.put(row, builder.toString());
	}

	public String getLog(){
		StringBuilder builder = new StringBuilder();
		List<Row> lines = new ArrayList<>();
		lines.addAll(log.keySet());
		Collections.sort(lines);
		for(Row line: lines){
			builder.append(log.get(line));
		}
		return builder.toString();
	}

	public void writeToFile(final String path){
		
		Path file = Paths.get(path.replace(":", "-")+".log");
		List<String> lines = Arrays.asList(getLog());
		
		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e);
		}
		

	}
}

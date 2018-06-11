package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.ArgumentTypeEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.InstanceOfPiPolicy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.*;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileUtil;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;
import com.ibm.wala.viz.PDFViewUtil;
import com.ibm.wala.util.graph.GraphSlicer;

import parser.Parser;


public class CustomSlicer {
	
	private CallGraph cg;
	private CallGraphBuilder cgBuilder;
	private String cgBuildTime;
	private String sliceTime;
	private String cgSize;
	private String sliceSize;
	private String sdgSize;
	private String sdgEdges;
	
	private static String TIME_UNIT_MS = " ms";
	private static String CG_UNITS = " callgraph nodes";
	private static String SDG_UNITS = " sdg nodes";
	private static String SDG_EDGE_UNITS = " sdg edges";
	private static String SLICE_TEXT = " statements in slice";
	
	public void resetValues(){
		cgBuilder = null;
		cg = null;
		cgBuildTime = null; 
		sliceTime = null;
		cgSize = null;
		sliceSize = null;
		sdgSize = null;
	}
		
	public void setUpCallGraphBuilder(final AnalysisOptions options, final AnalysisCache cache, 
			final ClassHierarchy cha, final AnalysisScope scope){

		cgBuilder = Util.makeZeroOneCFABuilder(options, cache, cha, scope);	
	}
	
	public void buildCallGraph(final AnalysisOptions options, final AnalysisCache cache, 
			final ClassHierarchy cha, final AnalysisScope scope) 
					throws IllegalArgumentException, CallGraphBuilderCancelException{
		StopWatch sw = new StopWatch();
		sw.start();
		setUpCallGraphBuilder(options, cache, cha, scope);
		CallGraph cg = cgBuilder.makeCallGraph(options, null);
		sw.stop();
		long time = sw.getTime();
		cgBuildTime = time + TIME_UNIT_MS;
		this.cg = cg;
		cgSize = this.cg.getNumberOfNodes() + CG_UNITS;
	}
	
	public String getCGSize(){
		return cgSize;
	}
		
	public String getSliceSize(){
		return sliceSize;
	}
	
	public String getCGBuildTime(){
		return cgBuildTime;
	}
	
	public String getSliceTime(){
		return sliceTime;
	}
	
	public String getSDGSize(){
		return sdgSize;
	}
	
	public String getSDGEdgeNumber(){
		return sdgEdges;
	}
	
	public String getCGStatistics(){
		return CallGraphStats.getStats(cg);
	}
	
	public String getCGString(){
		return cg.toString();
	}
	
	public void doSlicing(String appJar, String mainClass, boolean only_public_entry, String srcCaller, String srcCallee, String srcCalleeClass,
			String javaSourcefolder, String output_dir, boolean split_java_output, boolean multiple_caller, DataDependenceOptions dOptions, ControlDependenceOptions cOptions, Properties prop) 
					throws WalaException, IOException, IllegalArgumentException, CancelException {	

		// Some output directory configuration
		if (output_dir==null || output_dir==""){
			output_dir = "output" + File.separatorChar;
		}
		else {
			output_dir = output_dir + File.separatorChar;
		}
		
		String java_output_dir = output_dir + File.separatorChar + "reconstructed_java";
		
		Boolean success_creating_folder = (new File(output_dir)).mkdirs();
		if (!success_creating_folder) {
		    // Directory creation failed
			System.out.println("Couldn't create output folder '" + output_dir + "'\nProbably no rights or already existing.");
		}
		Boolean success_creating_folder2 = (new File(java_output_dir)).mkdirs();
		if (!success_creating_folder2) {
		    // Directory creation failed
			System.out.println("Couldn't create output folder '" + java_output_dir + "'\nProbably no rights or already existing.");
		}
		
		StopWatch sw_full = new StopWatch();
		String sliceTime_full;
		sw_full.start();
		
		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider())
				.getFile("exclusions.txt"));

		ClassHierarchy cha = ClassHierarchy.make(scope);
		
		System.out.println("Class Hierarchy has " + cha.getNumberOfClasses() + " classes");
		//Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
		
		System.out.println("\n== GET ENTRY POINTS ==\n");
		// Getting Entrypoints via different methods

		String usedMethod = "";
		Iterable<Entrypoint> entrypoints = findEntryPoints(scope, cha, mainClass, only_public_entry);

		if(!entrypoints.iterator().hasNext()){
			System.out.println("COULD NOT FIND ENTRYPOINTS - EXIT");
			return;
		}
		else{
			System.out.println("Number of found entrypoints: " +  size(entrypoints));
		}

		
		//Iterable<Entrypoint> entrypoints = getEntrypoints(cha);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		options.setReflectionOptions(ReflectionOptions.NONE);
		AnalysisCache cache = new AnalysisCache();
		buildCallGraph(options, cache, cha, scope);
		
		System.out.println("\n== CALL GRAPH ==\n");
		// PRINT CG IN SYSOUT
		//System.out.println(cg.toString());
		try{
		    PrintWriter writer = new PrintWriter(output_dir + "call_craph.txt", "UTF-8");
		    writer.println(cg.toString());
		    writer.close();
		} catch (IOException e) {
			System.out.println("Could not save call_craph.txt");
		}
		//System.out.println(pointanalysis.toString());
		System.out.println("Saved CG to call_craph.txt\n");
		Boolean create_pdfs = Boolean.parseBoolean(prop.getProperty("create_pdfs", "false"));
		if (create_pdfs==true){
			System.out.println("== GENERATE CALL GRAPH PDF ==\n");
			printCgAsPdf(cg);			
		}
		Statement statement;
		List<Statement> statements;
		
		System.out.println("== FIND CALLER/CALLEE ==\n");
		if (srcCaller.equals("MAIN")){
			statement = findCallTo(findMainMethod(cg), srcCallee);			
		}
		else {
			//CGNode callerNode = findMethod(cg, srcCaller);
			// PND Multiple Methodes
			//findMethod finds the Caller method in the CallGraph
			//CGNode callerNode = findMethod(cg, srcCaller, "");
			List<CGNode> callerNodes = findMethods(cg, srcCaller, multiple_caller);
			System.out.println("Found Caller " + srcCaller + " " + callerNodes.size() + " time(s).");
			statements = findCallsTo(callerNodes, srcCallee, srcCalleeClass, prop);
			//System.out.println("\nStatement '"+ srcCallee +"' found: x" + statements.size() + "\n");
			//statement = statements.get(0);
			Collection<Statement> slice = null;
			Set<Statement> slice_list = new HashSet<Statement>();
			
			
			//Create Backward slice
			StopWatch sw = new StopWatch();
			sw.start();
			PointerAnalysis<InstanceKey> pointanalysis = cgBuilder.getPointerAnalysis();
			System.out.println("\n== POINTER ANALYSIS ==\n");
			try{
			    PrintWriter writer = new PrintWriter(output_dir + "pointeranalysis.txt", "UTF-8");
			    writer.println(pointanalysis.toString());
			    writer.close();
			} catch (IOException e) {
				System.out.println("Could not save pointeranalysis.txt");
			}
			//System.out.println(pointanalysis.toString());
			System.out.println("Saved PA to pointeranalysis.txt\n");
			System.out.println("== SLICING ==\n");
			System.out.println("Computing backward slice...\n");
			
			
			// If memory overload while processing, we maybe want to see how many SDG nodes/edges
/*			SDG sdg1 = new SDG(cg, pointanalysis, ModRef.make(), dOptions, cOptions);
			EdgeCounter edges1 = new EdgeCounter(sdg1);
			edges1.countEdges(0);
			int number_of_sdg_nodes1 = edges1.getNumberOfNodes();
			System.out.println("# Edges: " + edges1.getNumberOfEdges());
			System.out.println("# Nodes: " + number_of_sdg_nodes1 + "\n");*/

			
			for(Statement sm: statements){ 
				slice = Slicer.computeBackwardSlice(sm, cg, pointanalysis, dOptions, cOptions);
				System.out.println(".\n");
				slice_list.addAll(slice);
			}
			//20170621
			//SDG sdg = new SDG(cg, cgBuilder.getPointerAnalysis(), ModRef.make(), dOptions, cOptions);
			//List<Statement> statements2 = new ArrayList<>(); 
			//statements2.add(statement);
			//slice = Slicer.computeSlice(sdg,statements,true);

			sw.stop();
			sliceTime = sw.getTime() + TIME_UNIT_MS;
			sliceSize = slice_list.size() + SLICE_TEXT;

			//System.out.println("[");
			try{
				PrintWriter writer = new PrintWriter(output_dir + "slice_raw.txt", "UTF-8");
				for (Statement sl : slice_list){
					//System.out.println(sl);					
					    writer.println(sl);
				}
				writer.close();	
			} catch (IOException e) {
				System.out.println("Could not save slice_raw.txt\n");
			}
			System.out.println("Saved raw slice to " + output_dir + "slice_raw.txt\n");
			//System.out.println("]");
			
			
			//Printing IR (Intermediate Representation) for slice
			
			
			//SDG sdg = new SDG(cg, cgBuilder.getPointerAnalysis(), ModRef.make(), dOptions, cOptions);
			//sdgSize = sdg.getNumberOfNodes() + SDG_UNITS;
			//EdgeCounter e = new EdgeCounter(sdg);
			//e.countEdges(0);
			//sdgEdges = e.getNumberOfEdges() + SDG_EDGE_UNITS;
			
			//System.out.println("# Edges: " + e.getNumberOfEdges());
			//System.out.println("# Nodes: " + e.getNumberOfNodes());
			
			//SDG PRINT
			SDG sdg = new SDG(cg, pointanalysis, ModRef.make(), dOptions, cOptions);
			try{
				EdgeCounter edges = new EdgeCounter(sdg);
				edges.countEdges(0);
				int number_of_sdg_nodes = edges.getNumberOfNodes();
				System.out.println("# Edges: " + edges.getNumberOfEdges());
				System.out.println("# Nodes: " + number_of_sdg_nodes + "\n");
				
			    Boolean sdg_pdf = Boolean.parseBoolean(prop.getProperty("sdg_pdf", "false"));
			    if (create_pdfs && sdg_pdf){
			    	
				    PrintWriter writer = new PrintWriter(output_dir + "sdg.txt", "UTF-8");
				    for (Object s : sdg){
				    	writer.println(s.toString());
				    }
				    writer.flush();
				    writer.close();
			    
			    	String dot_exe = prop.getProperty("dot_exe");
			    	String pdfview_exe = prop.getProperty("pdfview_exe");
			    	final String PDF_FILE = "SDG.pdf";			    	
			    	Graph<Statement> g = pruneSDG(sdg);
			    	// Printing SDH with over 500 nodes is nonsense
			    	if (number_of_sdg_nodes < 500){
			    		try{
			    			DotUtil.dotify(g, makeNodeDecorator(), output_dir+"temp_sdg.dt", output_dir+PDF_FILE, dot_exe);			    		
			    		} catch(java.lang.OutOfMemoryError e){
			    			System.out.println("Could not create sdg.pdf: "+e);
			    		}	
			    	} else {
			    		System.out.println("Over 500 SDG nodes, will not export SDG.pdf\n");
			    	}
			    	
			    	//This somehow never works on mac
			    	//PDFViewUtil.launchPDFView(PDF_FILE, pdfview_exe);
			    }

			    
			} catch (IOException e) {
				System.out.println("Could not save sdg.txt");
			}
			
			Map<String, Set<Integer>> sliceLineNumbers = new HashMap<>();
			List<Integer> allLineNumbers = new ArrayList<>();
			
			//Get Indexes from Slicelist
			sliceLineNumbers = dumpSlices(slice_list);
			//System.out.println(sliceLineNumbers.toString());
			
			//Print IR for Slice
			try {
				printIRAsList(slice_list, cha, prop);
			} catch (WalaException e) {
				System.out.println("Creating IR-PDF not possible: " + e);
			}
			
			
			for(Map.Entry<String, Set<Integer>> elem: sliceLineNumbers.entrySet()){
				allLineNumbers.addAll(elem.getValue());
			}
			Map<Integer, String> times = new HashMap<>();
			times.put(0, cgBuildTime);
			times.put(1, sliceTime);
			System.out.println("Callgraph & Slice buildtime: " + times);
			System.out.println(cgSize);
			System.out.println(sliceSize);
			//System.out.println(sdgSize);
			//System.out.println(sdgEdges);
			//System.out.println("\nAll line numbers:");
			//System.out.println(allLineNumbers);
			//System.out.println("\nAll slice line numbers:");
			//System.out.println(sliceLineNumbers);
			
			SliceMapper sm = new SliceMapper();
			Parser parser = null;
			StringBuilder builder = new StringBuilder();
			
			System.out.println("\n== RECONSTRUCTING CODE ==\n");
			//System.out.println("\n:\n");				
			
			for(Map.Entry<String, Set<Integer>> elem: sliceLineNumbers.entrySet()){
				//System.out.println("KEYVAL: "+ elem.getKey() + elem.getValue());
				// PND: Search java source in directory "java_source_folder" set by config
				String java_location = findFile(elem.getKey(), javaSourcefolder);
				if (java_location==""){
					java_location = elem.getKey();
				}
				Set<Integer> slicelinenum_sorted = new TreeSet<>(elem.getValue());	
				System.out.println("Slice line numbers (sorted) for file " + elem.getKey() + ": "+ slicelinenum_sorted);
				try{
					Set<Integer> mod_slice = parser.getModifiedSlice(java_location, elem.getValue());
					if (mod_slice != null){
						//System.out.println("Line numbers with Begin & EndLine: " + mod_slice);
						Set<Integer> mod_slice_sorted = new TreeSet<>(mod_slice);	
						//System.out.println("Line numbers with Begin & EndLine(sorted): " + mod_slice_sorted);
						builder.append(sm.getLinesOfCode(java_location, mod_slice));
						if (split_java_output & builder.length()!=1){
							saveReconstrJava(java_output_dir + File.separatorChar + elem.getKey(), builder);
							builder = new StringBuilder();
						}
					}
				}catch(Exception e1){
					System.out.println("Could not reconstruct code with parser for file '" + elem.getKey() + "': "  + e1);
				}
			}
			if (!split_java_output){
				saveReconstrJava(java_output_dir + File.separatorChar + "output_slice.java", builder);				
			}
			sw_full.stop();
			sliceTime_full = sw_full.getTime() + TIME_UNIT_MS;
			System.out.println("Full Slicing time: " +sliceTime_full);
		}
	}

	public static Set<Integer> dumpSlice(final Collection<Statement> slice){
		Set<Integer> numbers = new HashSet<>();
		for(Statement s: slice)
		if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
		  
			int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
			  try {
			    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
			    try {
			      int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
			      //System.out.println(s.getNode().getMethod().getDeclaringClass().getName());
			      System.out.println ( "Source line number = " + src_line_number );
			      numbers.add(src_line_number);
			    } catch (Exception e) {
			    }
			  } catch (Exception e ) {
			  }
			}
		return numbers;
	}
	
	public static Map<String, Set<Integer>> dumpSlices(final Collection<Statement> slice){
		Map<String, Set<Integer>> numbers = new HashMap<>();
		
		for(Statement s: slice)
			if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
		  
			int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
			IMethod met = s.getNode().getMethod();
			if (!(met instanceof ShrikeBTMethod)){
				continue;
			}
			try {
			    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
			    try {
			      int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
			      if (src_line_number==-1){
			    	  continue;
			      }
			      //System.out.println(" " + s.getNode().getMethod().getDeclaringClass().getName());
			      //System.out.println ( "Source line number = " + src_line_number + " with method " + s.getNode().getMethod());
			      String[] originalPath = s.getNode().getMethod().getDeclaringClass().getName().toString().split("/");
			      //System.out.println("origpath = " + s.getNode().getMethod().getDeclaringClass().getName().toString().split("/"));
			      String file = "";
			      //System.out.println("originalPath.length-1 =" + (originalPath.length-1));
			      if(originalPath.length-1 > 0){
			    	  file = originalPath[originalPath.length -1]+".java";
			      }
			      if(originalPath.length == 1){
			    	  file = originalPath[originalPath.length -1]+".java";
			    	  file = file.substring(1);
			    	  //System.out.println("Java File: "+ file);
			      }
			      if(numbers.get(file) == null){
			    	  numbers.put(file, new HashSet<Integer>());
			    	  Set<Integer> currentNumbers = numbers.get(file);
			    	  currentNumbers.add(src_line_number);
			    	  numbers.put(file, currentNumbers);
			      }
			      else{
			    	  Set<Integer> currentNumbers = numbers.get(file);
			    	  currentNumbers.add(src_line_number);
			    	  numbers.put(file, currentNumbers);
			      }
			    } catch (Exception e) {
			    	System.out.println("Error getting line numbers: " + e );
			    }
			  } catch (Exception e ) {
				  System.out.println("getBytecodeIndex handling failed: " + e);
			  }
			}
		return numbers;
	}

	
	/**
	 * aus de.gerken.bachelorarbeit.wala.slicer und angepasst.
	 * @param scope
	 * @param cha
	 * @param className
	 * @return
	 */
	public static Set<Entrypoint> getEntryPoints(AnalysisScope scope, IClassHierarchy cha, String className) {
		Set<Entrypoint> entryPoints = new HashSet<>();
		if(cha == null) {
			throw new IllegalArgumentException("cha is null");
		}
		
		for(IClass clazz : cha) {
			if(!clazz.isInterface()) {
				String typeName = clazz.getName().toString();
				
				if(className.contentEquals(typeName)) {
					if(isApplicationClass(scope, clazz)) {
						for(Iterator<IMethod> methodIt = clazz.getDeclaredMethods().iterator(); methodIt.hasNext();) {
							IMethod method = (IMethod) methodIt.next();
							if(!method.isAbstract()/* && method.isPublic()*/) {
								entryPoints.add(new ArgumentTypeEntrypoint(method, cha));
							}
						}
					}
				}
			}
		}
		return entryPoints;
	}
	
	public static Iterable<Entrypoint> findEntryPoints(AnalysisScope scope, ClassHierarchy cha, String mainClass, boolean only_public_entry) {
		String usedMethod = "";
		Iterable<Entrypoint> entrypoints = null;
		if (mainClass!= null && mainClass != ""){
			if (mainClass.startsWith("L") && only_public_entry == true){
				entrypoints = new SlicerEntrypoints(scope, cha, mainClass);
				usedMethod = "Public Entrypoints (Gulmann/Gerken)";
			}
			if(entrypoints == null || !entrypoints.iterator().hasNext()){
				entrypoints = getEntryPoints(scope, cha, mainClass);
				usedMethod = "getEntryPoints() (Detmers)";
			}
			if ((entrypoints == null || !entrypoints.iterator().hasNext()) && mainClass.startsWith("L")){
				entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
				usedMethod = "makeMainEntrypoints()";
			}
		}
		
		if(entrypoints == null || !entrypoints.iterator().hasNext()){
			entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
			usedMethod = "makeMainEntrypoints() without class parameter (WALA/Detmers)";
		}
		if(entrypoints == null || !entrypoints.iterator().hasNext()){
			entrypoints = new com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints(scope, cha);
			usedMethod = "AllApplicationEntrypoints() without class parameter";
		}

		if(!entrypoints.iterator().hasNext()){
			System.out.println("COULD NOT FIND ENTRYPOINTS - EXIT");
			return null;
		}
		else{
			System.out.println("Got Entrypoints using method: " + usedMethod);
			return entrypoints;
		}
	}
	
	
	public static CGNode findMainMethod(CallGraph cg) {
		Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
		Atom name = Atom.findOrCreateUnicodeAtom("main");
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		Assertions.UNREACHABLE("failed to find main() method");
		return null;
	}


	/**
	 * @param cg
	 * @param d
	 * @param name
	 * @return
	 */
	private static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		// if it's not a successor of fake root, just iterate over everything
		for (CGNode n : cg) {
			if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		Assertions.UNREACHABLE("failed to find method " + name);
		return null;
	}

	public static CGNode findMethod(CallGraph cg, String name) {
		Atom a = Atom.findOrCreateUnicodeAtom(name);
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode n = it.next();
			// PND Fix: class angeben
			if (n.getMethod().getName().equals(a)) {
					return n;
				//return n;
			}
		}
		//System.err.println("call graph " + cg);
		Assertions.UNREACHABLE("failed to find method " + name);
		return null;
	}  
	
	// PND
	public static List<CGNode> findMethods(CallGraph cg, String name, Boolean multiple) {
		Atom a = Atom.findOrCreateUnicodeAtom(name);
		List<CGNode> cgnodes = new ArrayList<>(); 
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(a)) {
				cgnodes.add(n);
				if (!multiple){
					return cgnodes;
				}
			}
		}
		if (cgnodes.size()==0){
			Assertions.UNREACHABLE("failed to find method " + name);
			return null;
		}
		return cgnodes;	
	}  
	
	public static Statement findCallTo(CGNode n, String methodName) {
		IR ir = n.getIR();
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
			SSAInstruction s = it.next();
			if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
				com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
				//System.out.println("call graph " + call.getCallSite().getDeclaredTarget().getName());
				if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
					com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
					com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
					return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
				}
			}
		}
		Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
		return null;
	}
	
	public static List<Statement> findCallsTo(List<CGNode> nodes, String methodName, String srcCalleeClass, Properties prop) {

		List<Statement> statements = new ArrayList<>(); 
		TypeReference call_class = null;
		Integer object_no = -1;
		List<Integer> object_nos = new ArrayList<>(); 
		Atom var_name  = null;
		for(CGNode n:nodes){
			IR ir = n.getIR();
			for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
				SSAInstruction s = it.next();
				//System.out.println("call name: "+ s + " class:" + s.getClass()); 
				if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
					com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
					//System.out.println("call name: " + call.getCallSite().getDeclaredTarget().getName().toString());
					if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
						if (srcCalleeClass!="" && !srcCalleeClass.isEmpty()){
							String clsname;
							if (srcCalleeClass.contains("/")){
								clsname = call.getCallSite().getDeclaredTarget().getDeclaringClass().getName().toString();
							}
							else {
								clsname = call.getCallSite().getDeclaredTarget().getDeclaringClass().getName().getClassName().toString();
							}
							if (clsname.equalsIgnoreCase(srcCalleeClass)){
								// When callee method found -> safe the object type in object to track it
								call_class = call.getCallSite().getDeclaredTarget().getDeclaringClass();
								com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
								com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
								Statement statement =  new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
								statements.add(statement);	
								System.out.println("Found Callee " + methodName + "() with object class: " + srcCalleeClass);
							}
						}
						else {
							// When callee method found -> safe the object type in object to track it
							call_class = call.getCallSite().getDeclaredTarget().getDeclaringClass();
							com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
							com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
							Statement statement =  new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
							statements.add(statement);	
							System.out.println("Found Callee " + methodName);
						}
					}
				}
			}
		}
		
		//Philip: Search for object creation and all methods
		Boolean advanced_mode = false;
		advanced_mode = Boolean.parseBoolean(prop.getProperty("advanced_mode", "false"));

		if (advanced_mode){
			System.out.println("\n== ADVANCED SLICING with object tracing ==\n");
			for(CGNode n:nodes){
				IR ir2 = n.getIR();

				for (Iterator<SSAInstruction> it = ir2.iterateAllInstructions(); it.hasNext();) {
					SSAInstruction s = it.next();
					
					//Try find instantiation of that object via "new" -> safe ID to object_no
					if (s instanceof com.ibm.wala.ssa.SSANewInstruction){
						com.ibm.wala.ssa.SSANewInstruction call = (com.ibm.wala.ssa.SSANewInstruction) s;
						if ((call.getNewSite().getDeclaredType() == call_class)){
							object_no = call.getDef();
							object_nos.add(call.getDef());
							System.out.println("new object_no: " + call.getDef() + " (new instruction)");
						}
					}
					
					//20180212: Check Phi statements for multiple definitions (same variable but in two different branches)
					Iterator allPhis = ir2.iteratePhis();
					for (Iterator<SSAPhiInstruction> i = allPhis; i.hasNext();){
						SSAPhiInstruction phi = i.next();
						int counter = 0;
						while (counter < phi.getNumberOfUses()){
							int phi_no = phi.getDef();
							int param = phi.getUse(counter);
							if (object_nos.contains(param) && !object_nos.contains(phi_no)){
								System.out.println("new object_no: " + phi_no + " (found in phi: old obj: "+ param +")");
								object_nos.add(phi.getDef());
							}
							counter++;
						}
					}
					
					
					//object eventually put into a variable
					if (s instanceof com.ibm.wala.ssa.SSAPutInstruction){
						com.ibm.wala.ssa.SSAPutInstruction call = (com.ibm.wala.ssa.SSAPutInstruction) s;
						if (call.getVal() == object_no){
							var_name = call.getDeclaredField().getName();
							System.out.println("new var_name: " + var_name + " (put)");
						}
					}
					
					//com.ibm.wala.ssa.SSAGetInstruction
					//object eventually get from a variable -> new link
					if (s instanceof com.ibm.wala.ssa.SSAGetInstruction){
						com.ibm.wala.ssa.SSAGetInstruction call = (com.ibm.wala.ssa.SSAGetInstruction) s;
						if (call.getDeclaredField().getName() == var_name){
							object_nos.add(call.getDef());
							System.out.println("new object_no: " + call.getDef() + " (get on " + var_name + ") ");
						}
					}
					
					//If not found try get instantiation via methods like getInstance(), later add parameters for those methods
					if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
						com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
						
						//When methods found on object type, check if it is instantiation of that object -> save ID to object_no
						//Also skip static method calls since they are class-methods
						if ((call.getCallSite().getDeclaredTarget().getDeclaringClass() == call_class)){
							Atom this_methodname = call.getCallSite().getDeclaredTarget().getName(); 
							List<String> instantiation_list = Arrays.asList(prop.getProperty("object_inst_methods").split(","));
							if (!instantiation_list.isEmpty()){
								for (String inst_method : instantiation_list){
									Atom inst_atom = Atom.findOrCreateUnicodeAtom(inst_method);
									if  (this_methodname == inst_atom){
										object_no = call.getDef();
										object_nos.add(call.getDef());
										System.out.println("new object_no: " + object_no + " (" + inst_method + ") ");
									}					
								}
							}
	
							//If there is a method call on that instantiated object (ID), add Statement to slice criteria
							//if (!(call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) && (call.getReceiver() == object_no) && !(call.getCallSite().isStatic())){
							if (!call.getCallSite().isStatic()){
								if (!(call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) && (object_nos.contains(call.getReceiver()))){	
									System.out.println("Found method call: " + call.getCallSite().getDeclaredTarget().getName().toString() +  " (on object " +  call_class + ")");
									com.ibm.wala.util.intset.IntSet indices = ir2.getCallInstructionIndices(call.getCallSite());
									Statement statement =  new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
									statements.add(statement);							
								}
							}
						}
					}
				}
			}				
			//Philip: End
			System.out.println("\n== END ADVANCED SLICING ==\n");
		}
		
		if (statements.size()==0){
			Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + nodes);
			return null;
		}
		else{
			return statements;
		}

	}
	
	
	private static boolean isApplicationClass(AnalysisScope scope, IClass clazz) {
		return scope.getApplicationLoader().equals(clazz.getClassLoader().getReference());
	}
	
	public void printIRAsList(final Collection<Statement> slice, ClassHierarchy cha, Properties prop) throws IOException, WalaException{
		List<IR> irs = new ArrayList<>();
	    final String PDF_FILE = "ir.pdf";
	    final String DOT_FILE = "temp_ir.dt";
		
		
		System.out.println("Number of Statements: " + slice.size());
		
		//System.out.println("\n== PRINT ALL IRs ==\n");
		
		for(Statement s: slice){
			if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
				 
				//PRINT SSA Instructions
				//System.out.println("\n== PRINT SSA INSTRUCTION FROM SLICE: ==\n" + s.toString());
				//System.out.println("Number of CFG nodes: " + s.getNode().getIR().getControlFlowGraph().getNumberOfNodes() + "");
				//System.out.println("Instructions in undefined order:\n");
				
				// for(Iterator<? extends SSAInstruction>  it = s.getNode().getIR().iterateAllInstructions(); it.hasNext();){
				for(Iterator<? extends SSAInstruction>  it = s.getNode().getIR().iterateNormalInstructions(); it.hasNext();){
					SSAInstruction instr = it.next();
					//PRINT SSA instructions
					//System.out.println(instr.toString());

				}
				//System.out.println("\n");
				//System.out.println("Basic blocks in cfg order:\n");
				for(Iterator<? extends ISSABasicBlock> it2 = s.getNode().getIR().getControlFlowGraph().iterator(); it2.hasNext();){
					ISSABasicBlock basicblock = it2.next();
					//PRINT Basic Blocks
					//System.out.println(basicblock.toString());
					for (Iterator<? extends SSAInstruction> it3 = basicblock.iterator(); it3.hasNext();){
						SSAInstruction instr = it3.next();
						
						//Also print the instructions within the basic block
						//System.out.println("    " + instr.toString());
						//System.out.println(instr.getDef());
					}		
				}
			}
		}
		
		//Test IR TO PDF
	    Boolean create_pdfs = Boolean.parseBoolean(prop.getProperty("create_pdfs", "false"));
	    if (create_pdfs){
	    	String dot_exe = prop.getProperty("dot_exe");
	    	String pdfview_exe = prop.getProperty("pdfview_exe");
		    IR ir = slice.iterator().next().getNode().getIR(); 
		    PDFViewUtil.ghostviewIR(cha, ir, PDF_FILE, DOT_FILE, dot_exe, pdfview_exe);
	    }
	    return;			
	}
	
    public void printCgAsPdf(Graph<CGNode> g) throws IllegalArgumentException, CancelException, IOException {
	    try {
	      final String PDF_FILE = "cg.pdf";
	      final String DOT_FILE = "temp.dt";
	      
	      InputStream input = new FileInputStream("slicer.conf");

	  	  // load a properties file
	      Properties prop = new Properties();
	      prop.load(input);

		  Boolean create_pdfs = Boolean.parseBoolean(prop.getProperty("create_pdfs", "false"));
		  if (create_pdfs){
			  String output_dir = prop.getProperty("output_dir");
			  String dot_exe = prop.getProperty("dot_exe");
			  String pdfview_exe = prop.getProperty("pdfview_exe");
			  String pdfFile = "";
			  String dotFile = "";
			  
			  if (output_dir==null || output_dir==""){
				  pdfFile = "output" + File.separatorChar + PDF_FILE;
				  dotFile = "output" + File.separatorChar + DOT_FILE;
			  }
			  else {
				  pdfFile = output_dir + File.separatorChar + PDF_FILE;
				  dotFile = output_dir + File.separatorChar + DOT_FILE;
			  }
			  
			  DotUtil.dotify(g, null, dotFile, pdfFile, dot_exe);
			  //PDFViewUtil.launchPDFView(System.getProperty("user.dir") + File.separatorChar + pdfFile, pdfview_exe);
		  }
	      return;

	    } catch (WalaException e) {
	      e.printStackTrace();
	      return;
	    }
	  }
    
    private static Graph<Statement> pruneSDG(final SDG<?> sdg) {
        Predicate<Statement> f = new Predicate<Statement>() {
          @Override public boolean test(Statement s) {
            if (s.getNode().equals(sdg.getCallGraph().getFakeRootNode())) {
              return false;
            } else if (s instanceof MethodExitStatement || s instanceof MethodEntryStatement) {
              return false;
            } else {
              return true;
            }
          }
        };
        return GraphSlicer.prune(sdg, f);
    }

    private static NodeDecorator<Statement> makeNodeDecorator() {
        return new NodeDecorator<Statement>() {
          @Override
          public String getLabel(Statement s) throws WalaException {
            switch (s.getKind()) {
            case HEAP_PARAM_CALLEE:
            case HEAP_PARAM_CALLER:
            case HEAP_RET_CALLEE:
            case HEAP_RET_CALLER:
              HeapStatement h = (HeapStatement) s;
              return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
            case EXC_RET_CALLEE:
            case EXC_RET_CALLER:
            case NORMAL:
            case NORMAL_RET_CALLEE:
            case NORMAL_RET_CALLER:
            case PARAM_CALLEE:
            case PARAM_CALLER:
            case PHI:
            default:
              return s.toString();
            }
          }

        };
      }
    public static String findFile(String file_name, String root_path) {
        try {
            boolean recursive = true;
            if (root_path==null){
            	return "";
            }
            Collection files = FileUtil.listFiles(root_path, null, recursive);

            for (Iterator iterator = files.iterator(); iterator.hasNext();) {
                File file = (File) iterator.next();
                if (file.getName().equals(file_name)){
                	System.out.println("Found file at: "+ file.getAbsolutePath());
                	return file.getAbsolutePath();                	
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		return "";
    }
    
    
    public static void saveReconstrJava(String file, StringBuilder builder){
    	try{
    		PrintWriter writer = new PrintWriter(file, "UTF-8");
    		writer.println(builder);
    		writer.close();
    		System.out.println("Saved reconstructed java to "+ file +"\n");
    	} catch (IOException e) {
    		System.out.println("Could not save " + file + "\n" + e);
    	}
    }
    
    public int size(Iterable<?> it) {
    	  if (it instanceof Collection)
    	    return ((Collection<?>)it).size();

    	  // else iterate

    	  int i = 0;
    	  for (Object obj : it) i++;
    	  return i;
    	}
}	

    



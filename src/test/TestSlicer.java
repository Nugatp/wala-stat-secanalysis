package test;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class TestSlicer {
	
	 public static void doSlicing(String appJar) throws WalaException, IOException, IllegalArgumentException, CancelException {
	      // create an analysis scope representing the appJar as a J2SE application
	        //AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar,CallGraphTestUtil.REGRESSION_EXCLUSIONS);
		    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider()).getFile("exclusions.txt"));
	        ClassHierarchy cha = ClassHierarchy.make(scope);

	        Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
	        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

	        // build the call graph
	        com.ibm.wala.ipa.callgraph.CallGraphBuilder cgb = Util.makeZeroCFABuilder(options, new AnalysisCache(),cha, scope, null, null);
	        CallGraph cg = cgb.makeCallGraph(options, null); // zweiter Parameter Monitorkrams
	        PointerAnalysis pa = cgb.getPointerAnalysis();

	        //Philip testin
	        //DataDependenceOptions opt1 = "main";
	        //DataDependenceOptions opt2 = "do_2_2_0";
/*	        SDG sdg = new SDG(cg, cgb.getPointerAnalysis());
	        try {
	          GraphIntegrity.check(sdg);
	        } catch (UnsoundGraphExcion e1) {
	          e1.printStackTrace();
	          Assertions.UNREACHABLE();
	        }
	        System.err.println(sdg);*/
	        
	        // find seed statement
	        Statement statement = findCallTo(findMainMethod(cg), "println");

	        Collection<Statement> slice1 = null;
	        Collection<Statement> slice2 = null;
	        Collection<Statement> slice3 = null;
	        
	        try {
		        // context-sensitive traditional slice
		        slice1 = Slicer.computeBackwardSlice (statement, cg, pa );
		        dumpSlice(slice1);
	
		        // context-sensitive thin slice
		        slice2 = Slicer.computeBackwardSlice(statement, cg, pa, DataDependenceOptions.NO_BASE_PTRS,
		            ControlDependenceOptions.NONE);
		        dumpSlice(slice2);
	
		        // context-insensitive slice
		        ThinSlicer ts = new ThinSlicer(cg,pa);
		        slice3 = ts.computeBackwardThinSlice ( statement );
		        dumpSlice(slice3);
	        }
		        
	        catch 
	        	(Exception e){}
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

	    public static Statement findCallTo(CGNode n, String methodName) {
	      IR ir = n.getIR();
	      for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
	        SSAInstruction s = it.next();
	        if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
	          com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
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

	    public static void dumpSlice(Collection<Statement> slice) {
	      for (Statement s : slice) {
	        System.err.println(s);
	      }
	    }
}

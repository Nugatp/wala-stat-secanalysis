package parser;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Parser {

	public static CompilationUnit getCu(final String javaPath) throws IOException {
		try {
			FileInputStream in = new FileInputStream(javaPath);	
			CompilationUnit cu = null;
			cu = JavaParser.parse(in);
			in.close();
			return cu;
		}catch (FileNotFoundException | ParseException e){
			//e.printStackTrace();
		}

		return null;
	}
	
	public static Set<Integer> getModifiedSlice(final String javaPath, final Set<Integer> sliceLines){
		MethodVisitor visitor = new MethodVisitor(sliceLines);

		try {
			CompilationUnit compilation_unit = getCu(javaPath);
			if(compilation_unit != null){
			    //System.out.println("Compilation unit:");
				//System.out.println(compilation_unit);
				visitor.visit(compilation_unit, "");
				return visitor.getSlice();
			}
			else{
				System.out.println(javaPath + " file not found! Skipping!\n");
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Set<Integer> emptyResult = new HashSet<>();
		return emptyResult;
	}
}

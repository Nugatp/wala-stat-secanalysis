package parser;

import japa.parser.ast.Node;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.TryStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import japa.parser.ast.stmt.Statement;

import java.util.HashSet;
import java.util.List;

public class MethodVisitor extends VoidVisitorAdapter<Object> {

	private Set<Integer> slice = new HashSet<>();

	private Set<Integer> inSlice = new HashSet<>();

	public MethodVisitor(final Set<Integer> inSlice){
		super();
		this.inSlice = inSlice;
	}

	public Set<Integer> getStatementBody(Node node, int line, Set<Integer> exeSlice){
		if(node instanceof ForStmt || node instanceof SwitchStmt || node instanceof WhileStmt || node instanceof IfStmt || node instanceof TryStmt || node instanceof BlockStmt){
			if(node.getBeginLine() <= line && node.getEndLine() >= line){
				exeSlice.add(node.getBeginLine());
				exeSlice.add(node.getEndLine());
				if(node instanceof ForStmt){
					ForStmt forStatement = (ForStmt) node; 
					exeSlice.add(forStatement.getBody().getBeginLine());
					exeSlice.add(forStatement.getBody().getEndLine());
					if(forStatement.getBody() instanceof BlockStmt){
						exeSlice.addAll(getStatementBody(forStatement.getBody(),line,exeSlice));
					}
				}
				if(node instanceof WhileStmt){
					WhileStmt whileStatement = (WhileStmt) node; 
					exeSlice.add(whileStatement.getBody().getBeginLine());
					exeSlice.add(whileStatement.getBody().getEndLine());
					if(whileStatement.getBody() instanceof BlockStmt){
						exeSlice.addAll(getStatementBody(whileStatement.getBody(),line,exeSlice));
					}
				}
				if(node instanceof IfStmt){
					IfStmt ifStatement = (IfStmt) node; 
					int then_last_line = 0;
					if(ifStatement.getThenStmt() != null){
						// if no bracets, no inner blocks and probably statement without bracets
						if(ifStatement.getThenStmt().toString().contains("{")){
							exeSlice.add(ifStatement.getThenStmt().getBeginLine());
							then_last_line = ifStatement.getThenStmt().getEndLine();
							exeSlice.add(then_last_line);							
						}
						if(ifStatement.getThenStmt().getBeginLine() <= line && ifStatement.getThenStmt().getEndLine() >= line){
							exeSlice.addAll(getStatementBody(ifStatement.getThenStmt(),line,exeSlice));
						}
					}
					if(ifStatement.getElseStmt() != null){
						// if no bracets, no inner blocks and probably statement without bracets
						if(ifStatement.getThenStmt().toString().contains("{")){
							exeSlice.add(ifStatement.getElseStmt().getBeginLine());
							exeSlice.add(ifStatement.getElseStmt().getEndLine());
							List<Integer> lines = IntStream.rangeClosed(then_last_line, ifStatement.getElseStmt().getBeginLine()).boxed().collect(Collectors.toList());
							exeSlice.addAll(lines);			
						}
						if(ifStatement.getElseStmt().getBeginLine() <= line && ifStatement.getElseStmt().getEndLine() >= line){
							if(!ifStatement.getElseStmt().toString().contains("else")){
								exeSlice.add(ifStatement.getElseStmt().getBeginLine()-1);
								//exeSlice.add(ifStatement.getElseStmt().getEndLine());
							}	
							exeSlice.addAll(getStatementBody(ifStatement.getElseStmt(),line,exeSlice));
						}									
					}
				}
				
				if (node instanceof TryStmt){
					TryStmt trystmt = (TryStmt) node;
					//exeSlice.addAll(getStatementBody(trystmt.getTryBlock(),line,exeSlice));
					exeSlice.remove(node.getEndLine());
					for (Node child:trystmt.getChildrenNodes()){
						exeSlice.addAll(getStatementBody(child,line,exeSlice));
					}
					
				}
				if (node instanceof BlockStmt){
					BlockStmt blockstmt = (BlockStmt) node;
				
					for (Statement stmt:blockstmt.getStmts()){
						Node blocknode = (Node) stmt;
						exeSlice.addAll(getStatementBody(blocknode,line,exeSlice));
					}
				}
				if (node instanceof CatchClause){
					CatchClause catchstmt = (CatchClause) node;
					//exeSlice.add(catchstmt.getCatchBlock().getBeginLine());
					//exeSlice.add(catchstmt.getCatchBlock().getEndLine());
				}
				
			}
		}
		return exeSlice;
	}
	
	
	@Override
	public void visit(MethodDeclaration n, Object arg) {
		Set<Integer> exeSlice = new HashSet<>();
		List<Statement> nodes;
		boolean go = false;
		for (Integer line:inSlice){
			if(n.getBeginLine() <= line && n.getEndLine() >= line){
				go = true;				
			}
		}
		if (go == false){
			return;
		}
		
		//Fix Philip
		//Setting Class Body
		Integer first_line;
		if(n.getParentNode().toString().startsWith("@")){ 
			first_line = n.getParentNode().getBeginLine()+1;
		} else{
			first_line = n.getParentNode().getBeginLine();
		}

		exeSlice.add(first_line);	
		if(!n.getParentNode().toString().contains("{")){
			exeSlice.add(first_line+1);
		}
		// Add all lines between class and first node (Fix for backets in nextline)
		List<Node> children = n.getParentNode().getChildrenNodes();
		if (!children.isEmpty()){
			int first_body_index = 0;
			for (Node child:children){
				if (child instanceof ClassOrInterfaceType){
					++first_body_index;
				}
				else break;
			}
			List<Integer> lines = IntStream.rangeClosed(first_line, children.get(first_body_index).getBeginLine()-1).boxed().collect(Collectors.toList());
			exeSlice.addAll(lines);
		}
		
		//End Fix Philip
		exeSlice.add(n.getParentNode().getEndLine());
		nodes = n.getBody().getStmts();

		if (nodes == null){
			exeSlice.addAll(inSlice);
			slice.addAll(exeSlice);
			return;
		}
		
		for(Node node: nodes){ 
			for(Integer line: inSlice){
				if(node.getBeginLine() <= line && node.getEndLine() >= line){
					// Fix Philip
					exeSlice.add(n.getBeginLine());
					exeSlice.add(n.getEndLine());
					// END Fix Philip
					// PND 20180213
					exeSlice.add(n.getBody().getBeginLine());
					exeSlice.add(n.getBody().getEndLine());
					// END PND
					
					// Add all lines between method and first brackets (Fix for multiple line method heads)
					List<Integer> lines = IntStream.rangeClosed(n.getBeginLine(), n.getBody().getBeginLine()).boxed().collect(Collectors.toList());
					exeSlice.addAll(lines);
					
					exeSlice.addAll(getStatementBody(node, line, exeSlice));
					
				}
			}
		}
		exeSlice.addAll(inSlice);
		slice.addAll(exeSlice);
	}
	
	// Fix Philip: ConstructorDeclaration was ignored and led to wrong reconstructed code
	@Override
	public void visit(ConstructorDeclaration n, Object arg) {
		Set<Integer> exeSlice = new HashSet<>();
		List<Statement> nodes;
		boolean go = false;
		
		for (Integer line:inSlice){
			if(n.getBeginLine() <= line && n.getEndLine() >= line){
				go = true;				
			}
		}
		if (go == false){
			return;
		}
		
		//Setting Class Body
		Integer first_line;
		if(n.getParentNode().toString().startsWith("@")){ 
			first_line = n.getParentNode().getBeginLine()+1;
		}
		else{
			first_line = n.getParentNode().getBeginLine();
		}

		exeSlice.add(first_line);	
		if(!n.getParentNode().toString().contains("{")){
			exeSlice.add(first_line+1);
		}
		// Add all lines between class and constructor, because there is nothing in between (Fix for backets in nextline)
		List<Node> children = n.getParentNode().getChildrenNodes();
		if (children.size()>1){
			List<Integer> lines = IntStream.rangeClosed(first_line, children.get(0).getBeginLine()-1).boxed().collect(Collectors.toList());
			exeSlice.addAll(lines);
		}
		//End Fix Philip
		exeSlice.add(n.getParentNode().getEndLine());
		nodes = n.getBlock().getStmts();
		exeSlice.add(n.getBlock().getBeginLine());
		exeSlice.add(n.getBlock().getEndLine());
		
		if (nodes == null){
			exeSlice.addAll(inSlice);
			slice.addAll(exeSlice);
			return;
		}
		
		for(Node node: nodes){ 
			for(Integer line: inSlice){
				if(node.getBeginLine() <= line && node.getEndLine() >= line){
					// Fix Philip
					exeSlice.add(n.getBeginLine());
					exeSlice.add(n.getEndLine());
					// END Fix Philip
					
					// Add all lines between method and first brackets (Fix for multiple line method heads)
					List<Integer> lines = IntStream.rangeClosed(n.getBeginLine(), n.getBlock().getBeginLine()).boxed().collect(Collectors.toList());
					exeSlice.addAll(lines);
					
					exeSlice.addAll(getStatementBody(node, line, exeSlice));
				}
			}
		}

		exeSlice.addAll(inSlice);
		slice.addAll(exeSlice);
	}
	
	public Set<Integer> getSlice(){
		return slice;
	}
}



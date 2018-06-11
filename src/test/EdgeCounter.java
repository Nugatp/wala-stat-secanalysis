package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public class EdgeCounter {
	
	SDG sdg;
	private final Set<Integer> visited;
	private final Set<Edge> edges;
	public final Set<Integer> toVisit;
	public static Set<Edge> testEdges = new HashSet<>();
	public Set<Integer> visitedNodes;
	int i = 0;

	public EdgeCounter(final SDG sdg){
		this.sdg = sdg;
		this.visited = new HashSet<>();
		this.edges = new HashSet<>();
		this.toVisit = new HashSet<>();
		this.visitedNodes = new HashSet<>();
	}
	
	
	/**
	 * initialisier mit nodeId 0
	 * @param node
	 * @return
	*/ 
	public void countEdges(final int source){
		IntSet succs = sdg.getSuccNodeNumbers(sdg.getNode(source));
		IntIterator i = succs.intIterator();	
		visited.add(source);
		while(i.hasNext()){
			int target = i.next();
			//if(!(edges.contains(new Edge(source, target)) && edges.contains(new Edge(target, source)))){
			edges.add(new Edge(source, target));
			if(!visited.contains(target)){
				countEdges(target);
			}
		}
	}
	
	/**
	 * Nur zum Testen
	 * @param n
	 */
	public void cE(final Node n){
		Iterator<Node> i =n.succ.iterator();
		int source = n.id;
		System.out.println("s :"  + source);
		visitedNodes.add(source);
		System.out.println(visitedNodes.toString());
		while(i.hasNext()){
			Node targetNode = i.next();
			int target = targetNode.id;
			testEdges.add(new Edge(source, target));
			
			if(!visitedNodes.contains(target)){
				cE(targetNode);
			}
		}
	}
	
	public int getNumberOfEdges(){
		return edges.size();
	}
	
	public int getNumberOfNodes(){
		return visited.size();
	}
	
	public static void main(String[] argsv){
		EdgeCounter e = new EdgeCounter(null);
		Node a = new Node(2);
		Node b = new Node(1);
		Node x = new Node(0);
		x.addNode(a);
		a.addNode(b);
		b.addNode(x);
		System.out.println("visited: " + e.visitedNodes.toString());
		e.cE(x);
		System.out.println(testEdges.toString());
		
	}
}

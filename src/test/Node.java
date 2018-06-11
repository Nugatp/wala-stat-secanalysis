package test;

import java.util.ArrayList;
import java.util.List;

public class Node {

	public final int id; 
	public final List<Node> succ;
	public Node(int id){
		this.id = id;
		this.succ = new ArrayList<>();
	}
	
	public void addNode(Node n){
		succ.add(n);
	}
}

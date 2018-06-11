package test;

public class Edge {
	
	final int source;
	final int target;
	
	public Edge(final int source, final int target){
		this.source = source;
		this.target = target;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof Edge){
			
			if(((Edge) other).target == this.target && ((Edge) other).source == this.source)
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
	    StringBuffer buffer = new StringBuffer();
	    buffer.append("source " + this.source);
	    buffer.append("target " + this.target);
	    return buffer.toString().hashCode();
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(this.source);
		builder.append("|");
		builder.append(this.target);
		builder.append(")");
		return builder.toString();
	}
}


package logger;

public class Row implements Comparable<Row>{
	
	private int row;
	
	public Row(final int value){
		row = value;
	}
	
	public int getLine(){
		return row;
	}	
	
	@Override
	public boolean equals(Object obj)
	  {
	    if (((Row)obj).getLine() == this.getLine() )
	        return true;
	    return false;
	  }

	@Override
	public int compareTo(Row row) {
        if (row.getLine() > getLine()) return -1;
        if (row.getLine() == getLine()) return 0;
        return 1;		
	}
}

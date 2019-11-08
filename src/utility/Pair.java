package utility;

import job.Task;

public class Pair<T1, T2>  {
	private T1 first;
	private T2 second;
	
	public Pair(T1 first, T2 second){
		this.first = first;
		this.second= second;
	}
	
	public Pair(){
		
	}

	public T1 getFirst() {
		return first;
	}

	public T2 getSecond() {
		return second;
	}

	public void setFirst(T1 aFirst) {
		this.first = aFirst;
	}

	public void setSecond(T2 aSecond) {
		this.second = aSecond;
	}

	@Override
	public String toString() {
		String firstString = null;
		String secondString = null;
		if (first instanceof Double) {
			firstString = String.format("%-15.5f", first);
		} else if (first instanceof Integer) {
			firstString = String.format("%-15d", first);
		} else {
			firstString = first.toString();
		}

		if (second instanceof Double) {
			secondString = String.format("%-15.5f", second);
		} else if (second instanceof Integer) {
			secondString = String.format("%-15d", second);
		} else {
			secondString = second.toString();
		}

		return firstString + secondString;
	}
	
	@Override
	public final boolean equals(final Object obj) {
       if(obj instanceof Pair<?,?>){
    	   Pair<?,?> typedObj = (Pair<?,?>)obj;
    	   if(typedObj.getFirst().equals(this.getFirst()) && 
    			   typedObj.getSecond().equals(this.getSecond())){
    		   return true;
    	   }
    	   else{
    		   return false;
    	   }
       }
       else
    	   return false;
       
	}
	
	@Override 
	public int hashCode(){
		return first.hashCode() + second.hashCode();
	}

	// @Override
	// public int compareTo(Pair<T1, T2> another) {
	// // TODO Auto-generated method stub
	// Double curSecond = (Double) second;
	// Double anotherSecond = (Double) another.getSecond();
	//
	// return curSecond.compareTo(anotherSecond);
	// }
}

package org.firepick;

/**
 * Threadsafe LIFO single-producer/multi-consumer cache that returns most recently posted value.
 */
public class LIFOCache<T> {
  private volatile long readCount = 0;
  private volatile long writeCount = 0;
  private Object [] values = new Object[2];

  public LIFOCache() {}

  public T peek() {
    synchronized(this) {
      int valueIndex = (int)(writeCount - readCount);
      T result;				
      if (valueIndex > 0) {	
	valueIndex = 1;	
	result = (T) values[valueIndex];	
      } else {			
	result = (T) values[0];
      }			
      return result;
    }
  }

  public T get() {
    synchronized(this) {
      int valueIndex = (int)(writeCount - readCount);
      if (valueIndex > 0) {		
	valueIndex = 1;		
	values[0] = values[valueIndex];		
      }					
      readCount = writeCount;	
      T result = (T) values[0];
      return result;
    }
  }

  public void post(T value) {
    synchronized(this) {
      int valueIndex = (int)(writeCount - readCount + 1);
      if (valueIndex >= 2) {			
	valueIndex = 1; // overwrite existing
      }					
      values[valueIndex] = value;
      writeCount++;		
    }
  }

  public boolean isEmpty() {
    return writeCount == 0;
  }

  public boolean isFresh() { 
    return writeCount > 0 && writeCount != readCount; 
  }

}

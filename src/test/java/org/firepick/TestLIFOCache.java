package org.firepick;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// The following imports are required to run test content
import java.net.*;
import java.util.*;

public class TestLIFOCache extends TestCase {

  class MockValue<T> {
    private T value;
    public MockValue(T aValue) { value = aValue; }
    public MockValue() {}
    public MockValue(MockValue<T> that) { value = that.value; }
    public T getValue() { return value; }
  }

  public TestLIFOCache( String testName ) {
    super( testName );
  }

  public static Test suite() {
    return new TestSuite( TestLIFOCache.class );
  }

  public void testMockValue() throws Exception {
    MockValue<Integer> mockA = new MockValue<Integer>(123);
    MockValue<Integer> mockB = new MockValue<Integer>(mockA);
    assertEquals(123, (int)mockB.getValue());
  }

  public void testCache() throws Exception {
    LIFOCache<MockValue<Integer>> bufInt = new LIFOCache<MockValue<Integer>>();
    assert(!bufInt.isFresh());
    bufInt.post(new MockValue<Integer>(1));
    assert(bufInt.isFresh());

    assert(bufInt.isFresh());
    assertEquals(1, (int) bufInt.peek().getValue());
    assert(bufInt.isFresh());
    assertEquals(1, (int) bufInt.get().getValue());
    assert(!bufInt.isFresh());
    assertEquals(1, (int) bufInt.get().getValue());
    assert(!bufInt.isFresh());
    assertEquals(1, (int) bufInt.peek().getValue());
    assert(!bufInt.isFresh());
   
    bufInt.post(new MockValue<Integer>(2));
    assertEquals(2, (int) bufInt.get().getValue());
    bufInt.post(new MockValue<Integer>(3));
    bufInt.post(new MockValue<Integer>(4));
    assertEquals(4, (int) bufInt.get().getValue());

    LIFOCache<String> bufString = new LIFOCache<String>();
    assert(!bufString.isFresh());
    bufString.post("one");
    assert(bufString.isFresh());
    assertEquals("one", bufString.peek());
    assert(bufString.isFresh());
    assertEquals("one", bufString.get());
    assert(!bufString.isFresh());
    assertEquals("one", bufString.peek());
    assert(!bufString.isFresh());
    assertEquals("one", bufString.get());
    assert(!bufString.isFresh());

    bufString.post("two");
    assert(bufString.isFresh());
    assertEquals("two", bufString.peek());
    assert(bufString.isFresh());
    assertEquals("two", bufString.get());
    assert(!bufString.isFresh());
    assertEquals("two", bufString.get());
  }

}

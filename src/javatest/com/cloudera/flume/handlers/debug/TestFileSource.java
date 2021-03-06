/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.flume.handlers.debug;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.cloudera.flume.ExampleData;
import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventSource;

/**
 * These tests tests the simple FileSource reader
 */
public class TestFileSource implements ExampleData {

  @Test
  public void testFileSource() throws IOException {
    EventSource src = new TextFileSource(TEST);
    src.open();
    int count = 0;
    while (true) {
      Event e = src.next();
      if (e == null)
        break;
      count++;
      System.out.println(e);
    }
    System.out.println("read " + count + " lines");
    Assert.assertEquals(56, count);
    src.close();
  }

}

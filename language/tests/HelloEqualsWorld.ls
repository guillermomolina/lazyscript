/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function doIt(a) {
  println("Initial stack trace:");
  println(stacktrace());
  
  hello = 123;
  println("After 123 assignment:");
  println(stacktrace());
  
  helloEqualsWorld();
  println("After hello assignment:");
  println(stacktrace());
  
//  readln();
}

i = 0;
while (i < 10) {
  doIt(i);
  i = i + 1;
}


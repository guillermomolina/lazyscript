/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

 
function foo() {
  println(test(40, 2));
}

defineFunction("function test(a, b) { return a + b; }");
foo();

defineFunction("function test(a, b) { return a - b; }");
foo();


/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function add(a, b) {
  return a + b;
}

function sub(a, b) {
  return a - b;
}

function foo(f) {
  f(40, 2).println();
}

foo(add);
foo(sub);

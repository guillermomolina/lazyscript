/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function add(a, b) {
  return a + b;
}

function loop(n) {
  i = 0;
  while (i < n) {  
    i = add(i, 1);  
  }
  return i;
}

i = 0;
while (i < 20) {
  loop(1000);
  i = i + 1;
}
loop(1000).println();  

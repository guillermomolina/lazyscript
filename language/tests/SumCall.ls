/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function add(a, b) {
  return a + b;
}

function loop(n) {
  i = 0;  
  sum = 0;  
  while (i <= n) {  
    sum = add(sum, i);  
    i = add(i, 1);  
  }  
  return sum;  
}  

i = 0;
while (i < 20) {
  loop(10000);
  i = i + 1;
}
r = loop(10000);
r.println();  

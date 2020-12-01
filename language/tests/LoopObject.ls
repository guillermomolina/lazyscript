/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function loop(n) {
  obj = new();
  obj.i = 0;  
  while (obj.i < n) {  
    obj.i = obj.i + 1;  
  }  
  return obj.i;
}  

i = 0;
while (i < 20) {
  loop(1000);
  i = i + 1;
}
println(loop(1000));  


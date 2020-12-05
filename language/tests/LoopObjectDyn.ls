/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function loop(n, obj, name) {
  obj[name] = 0;  
  while (obj[name] < n) {  
    obj[name] = obj[name] + 1;  
  }  
  return obj[name];
}  

i = 0;
while (i < 20) {
  loop(1000, new(), "prop");
  i = i + 1;
}
loop(1000, new(), "prop").println();  


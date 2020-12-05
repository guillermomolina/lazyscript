/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function test(n) {
  a = 1;
  if (a > 0) {
    b = 10;
    b.println();
  } else {
    b = 20;
    a = 0;
    c = 1;
    if (b > 0) {
      a = -1;
      b = -1;
      c = -1;
      d = -1;
      print(d);
    }
  }
  b.println();
  a.println();
}
function main() {
  test(\"n_n\");
}

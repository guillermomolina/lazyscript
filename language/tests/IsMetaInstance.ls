/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function printTypes(type) {
  isInstance(type, 42).println();
  isInstance(type, 42000000000000000000000000000000000000000).println();
  isInstance(type, "42").println();
  isInstance(type, 42 == 42).println();
  isInstance(type, new()).println();
  isInstance(type, null).println();
  isInstance(type, null).println();
  "".println();
}

number = 42.type();
string = "42".type();
boolean = 42 == 42.type();
object = new().type();
f = printTypes.type();
nullType = null.type();

printTypes(number);
printTypes(string);
printTypes(boolean);
printTypes(object);
printTypes(f);
printTypes(nullType);

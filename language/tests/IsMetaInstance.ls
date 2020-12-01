/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

function printTypes(type) {
  println(isInstance(type, 42));
  println(isInstance(type, 42000000000000000000000000000000000000000));
  println(isInstance(type, "42"));
  println(isInstance(type, 42 == 42));
  println(isInstance(type, new()));
  println(isInstance(type, null));
  println(isInstance(type, null()));
  println("");
}

function null() {
}

number = typeOf(42);
string = typeOf("42");
boolean = typeOf(42 == 42);
object = typeOf(new());
f = typeOf(null);
null = typeOf(null());

printTypes(number);
printTypes(string);
printTypes(boolean);
printTypes(object);
printTypes(f);
printTypes(null);

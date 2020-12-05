/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

println();
"Hello World".println();
101.println();
null.println();
true.println();
false.println();
Object.println();
Null.println();
Object.prototype.println();
true.prototype.prototype.prototype.prototype.println();
"Hello World".println();
"Hello World".prototype.println();

a = 101 + 1;
a.println();

a = "Hello World";
a.println();
a = (Object.prototype == null);
a.println();
a = (true.prototype == True);
a.println();

Object.a = "c";
Object.a.println();

Object["b"] = "b";
Object["b"].println();

Object[Object.a] = "a";
Object[Object.a].println();

Object.d = () => { .println(); };
Object.d.println();
Object.d();

a = () => { .println(); };
a.println();
a();

function a(arg1) {
    arg1.println();
}

a(100);
a("Hello World");

a = ["Hello", 100];
a[0].println();
a[1].println();

a = {
    prototype: Array,
    b: () => { 
        "Hello World".println(); 
    },
    c: {
        "0": 1000
    },
    d: [ true, "AAA"]
};
a.println();
a.prototype.println();
a.b.println();
a.b();
a.c["0"].println();
a.d[0].println();

12.5.println();
(12.332/2.1).println();
(12.332/2).println();
(12/2.1).println();

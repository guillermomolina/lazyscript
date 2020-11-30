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

Object.d = () => { println(); };
Object.d.println();
Object.d();

a = () => { println(); };
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
    "a": "Hello",
    "b": 100
};
a.println();
a.a.println();
a.b.println();
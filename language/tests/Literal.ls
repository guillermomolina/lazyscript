println();
println("Hello World");
println(101);
println(null);
println(true);
println(false);
println(Object);
println(Null);
println(Object.prototype);
println(true.prototype.prototype.prototype.prototype);
println("Hello World");
println("Hello World".prototype);

a = 101 + 1;
println(a);

a = "Hello World";
println(a);
a = (Object.prototype == null);
println(a);
a = (true.prototype == True);
println(a);

Object.a = "c";
println(Object.a);

Object["b"] = "b";
println(Object["b"]);

Object[Object.a] = "a";
println(Object[Object.a]);

Object.d = () => { println(); };
println(Object.d);
Object.d();

a = () => { println(); };
println(a);
a();

function a(arg1) {
    println(arg1);
}

a(100);
a("Hello World");

a = ["Hello", 100];
println(a[0]);
println(a[1]);

a = {
    prototype: Array,
    b: () => { 
        println("Hello World"); 
    },
    c: {
        "0": 1000
    },
    d: [ true, "AAA"]
};
println(a);
println(a.prototype);
println(a.b);
a.b();
println(a.c["0"]);
println(a.d[0]);

println(12.5);
println(12.332/2.1);
println(12.332/2);
println(12/2.1);

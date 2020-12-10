"main.1".println();
this.x = 1;
"main.2".println();
x.println();
"main.3".println();

function f1(a) {
    "f1.1".println();
    this.x = this.x + a;
    "f1.2".println();
    function f2(b) {
        "f2.1".println();
        println();
        "f2.2".println();
        this.x = this.x + b;
        "f2.3".println();
    }
    "f1.3".println();
    f2(a);
    "f1.4".println();
    b1 = (b) => {
        "b1.1".println();
        println();
        "b1.2".println();
        this.x = this.x + b;
        "b1.3".println();
    };
    "f1.5".println();
    b1.println();
    "f1.6".println();
    b1.invoke(10);
    "f1.7".println();
}

"main.4".println();
f1.println();
"main.5".println();
//f1(10);
"main.6".println();
a = f1;
"main.7".println();
a(20);
"main.8".println();
x.println();
"main.9".println();

println();
this.x = 1;
x.println();

function f1(a) {
    println();
    this.x = x + a;
    function f2(b) {
        println();
        this.x = x + b;
    }
    f2(a);
    b1 = (b) => {
        b.println();
        //this.x = x + b;
    };
    b1.println();
    b1.invoke(a);
}

f1.println();
f1(10);
x.println();

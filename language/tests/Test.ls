println();
x = 1;
x.println();

function f1(a) {
    println();
    function f2(b) {
        println();
        x = x + b;
    }
    f2(a);
}

f1(10);
x.println();

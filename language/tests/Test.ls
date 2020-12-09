println();
x = 1;
x.println();

function f1(a) {
    println();
    function f2(a) {
        x = x + a;
    }
    f2(a);
}

f1(2);
x.println();

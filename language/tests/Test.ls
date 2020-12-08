x = 1;

function f1(a) {
    function f2(a) {
        x = x + a;
    }
    f2(a);
}

x.println();
f1(2);
x.println();

function f1(a) {
    b1 = (b) => {
        return b;
    };
    return b1.invoke(a);
}

f1(10).println();
f2 = f1;
f2(20).println();

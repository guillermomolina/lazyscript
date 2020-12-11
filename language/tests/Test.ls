function f1(a) {
    b1 = (b) => {
        return b;
    };
    return b1.invoke(a);
}

a = f1(10);
a.println();
f2 = f1;
a = f2(20);
a.println();

o = {
    a: 0,
    increment: function () { this.a = a + 1 } 
}
o.a = 0

while ( o.a < 5 ) {
    o.a.println()
    o.increment
    o.increment()
} 

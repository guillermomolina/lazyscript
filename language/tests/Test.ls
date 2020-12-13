a = 0
b = function (arg) { return arg + 1 }
while ( a < 10 ) {
    a.println()
    a = b(a)
    if( a > 5) {
        break
    }
} 

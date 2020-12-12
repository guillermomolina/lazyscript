a = { b: { c: function a(x) { return x + 10; } } }
a.b["d"] = 10
a.b.d.println()
a["b"]["c"](100).println()
a.b.e = [ 1 + 1, "a" ]
a.b.e[0].println()
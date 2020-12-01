/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

/* The easiest way to generate null: a function without a return statement implicitly returns null. */
function null() {
}

println(null());  
println(null() == null());  
println(null() != null());  
println(null() == 42);  
println(null() != 42);  
println(null() == "42");  
println(null() != "42");  

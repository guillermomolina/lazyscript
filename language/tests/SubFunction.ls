/*
 * Copyright (c) 2020, Guillermo Adrián Molina. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */


function z(d) {
    function y(c) {
        function x(b) {
            function w(a) {
                return a + 1;
            }
            return w(b) + 1;
        }
        return x(c) + 1;
    }
    return y(d) + 1;
}

println(z(10));

function w(a) {
    return a + 1;
}

function x(b) {
    return w(b) + 1;
}

function y(c) {
    return x(c) + 1;
}

function z(d) {
    return y(d) + 1;
}

println(z(10));

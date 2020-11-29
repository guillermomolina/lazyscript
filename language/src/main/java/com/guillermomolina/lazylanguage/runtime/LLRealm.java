package com.guillermomolina.lazylanguage.runtime;

import com.oracle.truffle.api.TruffleLanguage.Env;

public class LLRealm {

    private final LLContext context;
    private final Env env;

    public LLRealm(LLContext context, Env env) {
        this.context = context;
        this.env = env; // can be null
    }
}

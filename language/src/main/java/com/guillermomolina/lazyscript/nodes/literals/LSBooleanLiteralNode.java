package com.guillermomolina.lazyscript.nodes.literals;

import com.guillermomolina.lazyscript.nodes.LSExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

public class LSBooleanLiteralNode extends LSExpressionNode {

    private final boolean value;

    public LSBooleanLiteralNode(boolean value) {
        this.value = value;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return value;
    }    
}

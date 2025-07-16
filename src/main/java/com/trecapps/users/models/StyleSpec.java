package com.trecapps.users.models;

import com.fasterxml.jackson.databind.node.*;
import lombok.Data;

@Data
public class StyleSpec {
    String style;
    boolean useDark;

    public ObjectNode getObjectAsNode(){

        ObjectNode ret = new ObjectNode(new JsonNodeFactory(false));
        ret.set("style", new TextNode(style));
        ret.set("useDark", BooleanNode.valueOf(useDark));
        return ret;

    }
}

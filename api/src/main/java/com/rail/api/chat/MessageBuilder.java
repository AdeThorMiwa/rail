package com.rail.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageBuilder {

    private final ObjectMapper mapper;

    public ArrayNode blocks() {
        return mapper.createArrayNode();
    }

    public String toJson(ArrayNode blocks) {
        return blocks.toString();
    }

    public ObjectNode textSpan(String markdown) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "text");
        s.put("text", markdown);
        return s;
    }

    public ObjectNode ctaSpan(
        String label,
        String command,
        Map<String, Object> params
    ) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "cta");
        s.put("label", label);
        s.put("command", command);
        s.set("params", mapper.valueToTree(params));
        return s;
    }

    public ObjectNode textBlock(ObjectNode... spans) {
        return textBlock(List.of(spans));
    }

    public ObjectNode textBlock(List<ObjectNode> spans) {
        ObjectNode block = mapper.createObjectNode();
        block.put("type", "text");
        ArrayNode spansNode = block.putArray("spans");
        spans.forEach(spansNode::add);
        return block;
    }

    public ObjectNode tableBlock(List<List<String>> rows) {
        return tableBlock(null, rows);
    }

    public ObjectNode tableBlock(
        List<String> columns,
        List<List<String>> rows
    ) {
        ObjectNode block = mapper.createObjectNode();
        block.put("type", "table");
        if (columns != null) {
            block.set("columns", mapper.valueToTree(columns));
        } else {
            block.putNull("columns");
        }
        block.set("rows", mapper.valueToTree(rows));
        return block;
    }

    public ObjectNode actionsBlock(List<ObjectNode> items) {
        return actionsBlock(items, null, null, null, null);
    }

    public ObjectNode actionsBlock(
        List<ObjectNode> items,
        List<ObjectNode> successItems,
        List<ObjectNode> failureItems,
        String onSuccessEmpty,
        String onFailureEmpty
    ) {
        ObjectNode block = mapper.createObjectNode();
        block.put("type", "actions");
        ArrayNode itemsNode = block.putArray("items");
        items.forEach(itemsNode::add);
        if (successItems != null) {
            ArrayNode node = block.putArray("successItems");
            successItems.forEach(node::add);
        }
        if (failureItems != null) {
            ArrayNode node = block.putArray("failureItems");
            failureItems.forEach(node::add);
        }
        if (onSuccessEmpty != null) block.put("onSuccessEmpty", onSuccessEmpty);
        if (onFailureEmpty != null) block.put("onFailureEmpty", onFailureEmpty);
        return block;
    }

    public ObjectNode actionItem(
        String id,
        String label,
        String style,
        String command,
        Map<String, Object> params
    ) {
        ObjectNode item = mapper.createObjectNode();
        item.put("id", id);
        item.put("label", label);
        item.put("style", style);
        item.put("command", command);
        item.set("params", mapper.valueToTree(params));
        return item;
    }
}

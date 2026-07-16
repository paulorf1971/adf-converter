package com.tsystems.jira.kong.client.model.text.utils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 */
public final class AdfToMarkdownConverter {
	
    /**
     * Inline private Context class 
     */
    private static class Context {
        String listPrefix;
        
        public Context copy() {
            Context c = new Context();
            c.listPrefix = this.listPrefix;
            return c;
        }
    }
	
    /** */
    private AdfToMarkdownConverter() { }

    /**
     * 
     * @param adfRoot
     * @return
     */
    public static String toMarkdown(JsonNode adfRoot, CommentMediaGroupResolver mediaGroupResolver) {
        if (adfRoot == null || adfRoot.isNull()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        visit(adfRoot, sb, new Context());
        return normalize(sb.toString());
    }
    
    /**
     * 
     * @param node
     * @param sb
     * @param ctx
     */
    private static void visit(JsonNode node, StringBuilder sb, Context ctx) {
        if (node == null || node.isNull()) return;

        String type = node.path("type").asText();

        switch (type) {
            case "doc" -> visitChildren(node, sb, ctx);

            case "paragraph" -> {
                visitChildren(node, sb, ctx);
                sb.append("\n\n");
            }

            case "heading" -> {
                int level = node.path("attrs").path("level").asInt(1);
                sb.append("h").append(level).append(". ");
                visitChildren(node, sb, ctx);
                sb.append("\n\n");
            }

            case "bulletList" -> {
                ctx.listPrefix = "* ";
                visitChildren(node, sb, ctx);
                ctx.listPrefix = null;
                sb.append("\n");
            }

            case "orderedList" -> {
                ctx.listPrefix = "# ";
                visitChildren(node, sb, ctx);
                ctx.listPrefix = null;
                sb.append("\n");
            }

            case "listItem" -> {
                if (ctx.listPrefix != null) {
                    sb.append(ctx.listPrefix);
                }
                visitChildren(node, sb, ctx);
                sb.append("\n");
            }

            case "blockquote" -> {
                sb.append("bq. ");
                visitChildren(node, sb, ctx);
                sb.append("\n\n");
            }

            case "codeBlock" -> {
//                sb.append("{code}\n");
            	sb.append("{code}\n");
                visitChildren(node, sb, ctx);
//                sb.append("\n{code}\n\n");
            }           
            
            case "mediaSingle" -> appendMediaWithMarks(node, sb);
            
            case "table" -> appendTable(node, sb, ctx);
            
            case "hardBreak" -> sb.append("\n");

            case "text" -> appendTextWithMarks(node, sb);
            
            case "inlineCard" -> appendInlineUrlToText(node, sb);

            default -> visitChildren(node, sb, ctx);
        }
    }
    
    
    
    /**
     * 
     * @param node
     * @param sb
     */
    private static void appendTextWithMarks(JsonNode node, StringBuilder sb) {
        String text = node.path("text").asText("");
        JsonNode marks = node.path("marks");
        if (marks.isArray()) {
            for (JsonNode mark : marks) {
                switch (mark.path("type").asText()) {
                    case "strong" -> text = "**" + text + "**";
                    case "em" -> text = "_" + text + "_";
                    case "code" -> text = "`" + text + "`";
                    case "strike" -> text = "-" + text + "-";
                    case "link" -> {
                        String href = mark.path("attrs").path("href").asText();
                        text = "[" + text + "|" + href + "]";
                    }
                }
            }
        }

        sb.append(text);
    }
    
    /**
     * 
     * @param node
     * @param sb
     */
    private static void appendInlineUrlToText(JsonNode node, StringBuilder sb) {
    	String urlContent = node.path("attrs").path("url").asText();
    	String urlConverted = String.format("[%s](%s)", urlContent, urlContent);
    	sb.append(urlConverted);
    }
     
    
    private static void appendMediaWithMarks(JsonNode node, StringBuilder sb) {
    	String width = null;      
        JsonNode subContents = node.get("content");        
        if (subContents.isArray()) {
        	for (JsonNode subContent : subContents) {
        		JsonNode subAttrs = subContent.path("attrs");
        		String alt = subAttrs.path("alt").asText("");                
                if ( alt != null ) {
                	String mediaText = String.format("!^%s", alt);
                	String height = subAttrs.path("height").asText("100");
                	width = subAttrs.path("width").asText("100");
                	mediaText += String.format("|width=%s,height=%s!\n", width, height);
                	sb.append(mediaText);
                }
        	}
        }
    }
    
    
    private static void appendTable(JsonNode tableNode, StringBuilder sb, Context ctx) {
        for (JsonNode row : tableNode.path("content")) {

            boolean isHeaderRow = isHeaderRow(row);

            for (JsonNode cell : row.path("content")) {

                if (isHeaderRow) {
                    sb.append("|| ");
                } else {
                    sb.append("| ");
                }

                sb.append(renderTableCell(cell, ctx)).append(" ");
            }

            if (isHeaderRow) {
                sb.append("||");
            } else {
                sb.append("|");
            }

            sb.append("\n");
        }

        sb.append("\n");
    }
    
    /**
     * 
     * @param row
     * @return
     */
    private static boolean isHeaderRow(JsonNode row) {
        for (JsonNode cell : row.path("content")) {
            if (!"tableHeader".equals(cell.path("type").asText())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 
     * @param cell
     * @param ctx
     * @return
     */
    private static String renderTableCell(JsonNode cell, Context ctx) {
        StringBuilder cellBuffer = new StringBuilder();

        // clone context if it is mutable (recommended)
        Context cellCtx = ctx.copy(); // implement copy() if needed
        visitChildren(cell, cellBuffer, cellCtx);
        String text = cellBuffer.toString().trim();

        // Jira tables do NOT support real new lines inside cells
        text = text.replace("\n", "\\\\ ");
        return text;
    }
     
    /**
     * 
     * @param node
     * @param sb
     * @param ctx
     */
    private static void visitChildren(JsonNode node, StringBuilder sb, Context ctx) {
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                visit(child, sb, ctx);
            }
        }
    }
    
    /**
     * 
     * @param s
     * @return
     */
    private static String normalize(String s) {
        return s.replaceAll("\\n{3,}", "\n\n").trim();
    }
    
}


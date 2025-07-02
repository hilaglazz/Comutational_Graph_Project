package test;

import java.util.*;

public class HtmlGraphWriter {
    public static String getGraphHTML(Graph g) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Graph</title></head><body>");
        html.append("<h2>System Graph</h2>");
        html.append("<svg width='1000' height='800'>");

        // Positions for each node (row/col layout)
        int spacingX = 200;
        int spacingY = 120;
        Map<String, String> positions = new HashMap<>();
        int i = 0;

        for (Node node : g) {
            String name = node.getName();
            int x = spacingX * (i % 5) + 60;
            int y = spacingY * (i / 5) + 60;
            positions.put(name, x + "," + y);

            if (name.startsWith("T")) {
                // Rectangle for topic
                html.append(String.format("<rect x='%d' y='%d' width='80' height='40' fill='lightblue' stroke='black'/>\n", x - 40, y - 20));
            } else {
                // Circle for agent
                html.append(String.format("<circle cx='%d' cy='%d' r='25' fill='lightgreen' stroke='black'/>\n", x, y));
            }

            // Label
            html.append(String.format("<text x='%d' y='%d' text-anchor='middle' font-size='12'>%s</text>\n",
                    x, y + 40, name));

            i++;
        }

        // Arrowhead definition
        html.append("<defs><marker id='arrow' markerWidth='10' markerHeight='10' refX='10' refY='3' orient='auto'>"
                + "<path d='M0,0 L0,6 L9,3 z' fill='black'/></marker></defs>");

        // Draw edges
        for (Node from : g) {
            String[] fromPos = positions.get(from.getName()).split(",");
            int x1 = Integer.parseInt(fromPos[0]);
            int y1 = Integer.parseInt(fromPos[1]);

            for (Node to : from.getEdges()) {
                String[] toPos = positions.get(to.getName()).split(",");
                int x2 = Integer.parseInt(toPos[0]);
                int y2 = Integer.parseInt(toPos[1]);

                html.append(String.format(
                        "<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='black' stroke-width='2' marker-end='url(#arrow)' />\n",
                        x1, y1, x2, y2));
            }
        }

        html.append("</svg></body></html>");
        return html.toString();
    }
}

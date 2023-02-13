/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 * EIMBubbleBorder
 *
 * @author Denis Meyer
 */
public class EIMBubbleBorder extends AbstractBorder {

    private final Color color;
    private int thickness = 1;
    private int radius = 3;
    private int pointerSize = 5;
    private Insets insets = null;
    private BasicStroke stroke = null;
    private final int strokePad;
    private final int pointerPad = 4;
    private final RenderingHints hints;

    public EIMBubbleBorder(Color color, int thickness, int radius, int pointerSize) {
        this.thickness = thickness;
        this.radius = radius;
        this.pointerSize = pointerSize;
        this.color = color;

        stroke = new BasicStroke(thickness);
        strokePad = thickness / 2;

        hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = this.radius + strokePad;
        int bottomPad = pad + this.pointerSize + strokePad;
        insets = new Insets(pad, pad, bottomPad, pad);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        return getBorderInsets(c);
    }

    @Override
    public void paintBorder(
            Component c,
            Graphics g,
            int x, int y,
            int width, int height) {

        Graphics2D g2 = (Graphics2D) g;

        int bottomLineY = height - thickness - pointerSize;

        RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(
                0 + strokePad,
                0 + strokePad,
                width - thickness,
                bottomLineY,
                radius,
                radius
        );

        Polygon pointer = new Polygon();

        // left point
        pointer.addPoint(
                strokePad + radius + pointerPad,
                bottomLineY);
        // right point
        pointer.addPoint(
                strokePad + radius + pointerPad + pointerSize,
                bottomLineY);
        // bottom point
        pointer.addPoint(
                strokePad + radius + pointerPad + (pointerSize / 2),
                height - strokePad);

        Area area = new Area(bubble);
        area.add(new Area(pointer));

        g2.setRenderingHints(hints);

        Area spareSpace = new Area(new Rectangle(0, 0, width, height));
        spareSpace.subtract(area);
        g2.setClip(spareSpace);
        g2.clearRect(0, 0, width, height);
        g2.setClip(null);

        g2.setColor(color);
        g2.setStroke(stroke);
        g2.draw(area);
    }
}

/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui.components;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * EIMListCellRenderer
 *
 * @author Denis Meyer
 */
public class EIMListCellRenderer extends JLabel implements ListCellRenderer {

    private final Color color_first = new Color(227, 227, 227);
    private final Color color_second = new Color(202, 225, 255);
    private final Color color_selected = new Color(92, 172, 238);

    public EIMListCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());

        if (isSelected) {
            setBackground(color_selected);
            this.setBorder(new EIMBubbleBorder(color_selected.darker(), 1, 3, 0));
        } else {
            if (index % 2 == 0) {
                this.setBorder(new EIMBubbleBorder(color_second.darker(), 1, 3, 0));
                setBackground(color_first);
            } else {
                this.setBorder(new EIMBubbleBorder(color_first.darker(), 1, 3, 0));
                setBackground(color_second);
            }
        }

        return this;
    }
}

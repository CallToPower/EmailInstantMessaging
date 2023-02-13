/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui.components;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMCombobox
 *
 * @author Denis Meyer
 */
public class EIMCombobox extends JComboBox {

    private static final Logger logger = LogManager.getLogger(EIMCombobox.class.getName());

    private AutoTextFieldEditor autoTextFieldEditor;
    private boolean isFired;

    public EIMCombobox() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMCombobox");
        }
        init(new ArrayList<String>());
    }

    public boolean isCaseSensitive() {
        return autoTextFieldEditor.getAutoTextFieldEditor().isCaseSensitive();
    }

    public void setCaseSensitive(boolean flag) {
        autoTextFieldEditor.getAutoTextFieldEditor().setCaseSensitive(flag);
    }

    public boolean isStrict() {
        return autoTextFieldEditor.getAutoTextFieldEditor().isStrict();
    }

    public void setStrict(boolean flag) {
        autoTextFieldEditor.getAutoTextFieldEditor().setStrict(flag);
    }

    public List getDataList() {
        return autoTextFieldEditor.getAutoTextFieldEditor().getDataList();
    }

    public void setDataList(List list) {
        autoTextFieldEditor.getAutoTextFieldEditor().setDataList(list);
        setModel(new DefaultComboBoxModel(list.toArray()));
    }

    public void setSelectedValue(Object obj) {
        if (!isFired) {
            isFired = true;
            setSelectedItem(obj);
            fireItemStateChanged(new ItemEvent(this, 701, selectedItemReminder, 1));
            isFired = false;
        }
    }

    @Override
    protected void fireActionEvent() {
        if (!isFired) {
            super.fireActionEvent();
        }
    }

    private void init(List list) {
        isFired = false;
        autoTextFieldEditor = new AutoTextFieldEditor(list);
        setEditable(true);
        setModel(new DefaultComboBoxModel(list.toArray()) {
            @Override
            protected void fireContentsChanged(Object obj, int i, int j) {
                if (!isFired) {
                    super.fireContentsChanged(obj, i, j);
                }
            }
        });
        setEditor(autoTextFieldEditor);
    }

    private class AutoTextFieldEditor extends BasicComboBoxEditor {

        AutoTextFieldEditor(List list) {
            editor = new EIMTextField(list, EIMCombobox.this);
        }

        private EIMTextField getAutoTextFieldEditor() {
            return (EIMTextField) editor;
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.ScreenInfo;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import static com.myutils.logbrowser.inquirer.EnterRegexDialog.RET_OK;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author ssydoruk
 */
public class MyCheckBoxList extends CheckBoxList {

    private int lastIdx;

    private String lastRegEx = null;
    private JPanel jpo = null;
    private DefaultListModel lm = null;
    EnterRegexDialog dlg = null;

    MyChBoxListPopup menu;
    private ListSelectionListener[] listSelectionListeners = null;

    public MyCheckBoxList(JPanel jpo) {
        super();
        lm = new DefaultListModel();
        setModel(lm);
        this.jpo = jpo;
        jpo.setLayout(new BorderLayout());
        jpo.add(new JScrollPane(this));
        String act = "Search";
        getInputMap().put(KeyStroke.getKeyStroke('F', java.awt.event.InputEvent.CTRL_DOWN_MASK), act);
        getActionMap().put(act, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(true, true);
            }
        });

        act = "SearchForward";
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), act);
        getActionMap().put(act, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(false, true);
            }
        });

        act = "SearchBack";
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_DOWN_MASK), act);
        getActionMap().put(act, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch(false, false);
            }
        });

    }

    public MyCheckBoxList(DefaultListModel lmRefValues, JPanel jpo) {
        this(jpo);
        setModel(lmRefValues);
        lm = lmRefValues;
        menu = new MyChBoxListPopup(this, jpo);

        addMouseListener(new PopClickListener(menu));

    }

    public DefaultListModel getLm() {
        return lm;
    }

    public String getLastRegEx() {
        return lastRegEx;
    }

    void checkAll() {
        int[] indices = new int[lm.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        setCheckBoxListSelectedIndices(indices);
    }

    void SelectedUp() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex > 0) {
            boolean checked = isChecked(selectedIndex);
            Object remove = lm.remove(selectedIndex);
            lm.add(selectedIndex - 1, remove);
            setChecked(selectedIndex - 1, checked);
            setSelectedIndex(selectedIndex - 1);

        }
    }

    void SelectedDown() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < lm.size() - 1) {
            boolean checked = isChecked(selectedIndex);
            Object remove = lm.remove(selectedIndex);
            lm.add(selectedIndex + 1, remove);
            setChecked(selectedIndex + 1, checked);
            setSelectedIndex(selectedIndex + 1);
        }
    }

    private boolean isChecked(int selectedIndex) {
        return getCheckBoxListSelectionModel().isSelectedIndex(selectedIndex);
    }

    private void setChecked(int i, boolean checked) {
        if (checked) {
            getCheckBoxListSelectionModel().addSelectionInterval(i, i);
        }
    }

    private void doSearch(boolean initialRun, boolean searchForward) {
        Window windowAncestor = (Window) SwingUtilities.getWindowAncestor(this);
        if (dlg == null) {
            dlg = new EnterRegexDialog(null, true);
        }
        boolean runSearch = true;
        boolean isForward = searchForward;
        if (initialRun || lastIdx < 0) {
            Point location = windowAncestor.getMousePosition();
            dlg.setLocation(location);
            dlg.setRegex(inquirer.geLocaltQuerySettings().getSavedFilters());
            dlg.setShowUps(true);
            dlg.setDown(searchForward);
            ScreenInfo.setVisible(this, dlg, true);
            runSearch = dlg.getReturnStatus() == RET_OK;
            if (runSearch) {
                lastRegEx = dlg.getSearch();
                inquirer.geLocaltQuerySettings().saveRegex(lastRegEx);
                isForward = dlg.isDownChecked();
            }
        }
        if (runSearch) {
            ListModel model = getModel();
            int first = 0;
            first = getSelectedIndex();
            int incr;
            int end;
            if (isForward) {
                incr = 1;
                end = model.getSize();
            } else {
                incr = -1;
                end = -1;
            }
            if (first < 0) {
                first = (isForward) ? 0 : model.getSize();
            } else {
                first += incr;
            }

            for (int i = first; i != end; i += incr) {
                Object elementAt = model.getElementAt(i);
                if (!elementAt.equals(CheckBoxList.ALL_ENTRY)) {
                    if (dlg.checkMatch(elementAt.toString())) {
                        setSelectedIndex(i);
                        ensureIndexIsVisible(i);
                        lastIdx = i;
                        return;
                    }
                }
            }

            JOptionPane.showMessageDialog(windowAncestor, "No items found", "info", INFORMATION_MESSAGE);
        }
    }

    void initFullUpdate() {
        setValueIsAdjusting(true);
        listSelectionListeners = getCheckBoxListSelectionModel().getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            getCheckBoxListSelectionModel().removeListSelectionListener(listSelectionListener);
        }

        lm.removeAllElements();
    }

    void doneFullUpdate(boolean shouldCheckAll) {
        lm.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
        if (shouldCheckAll) {
            getCheckBoxListSelectionModel().addSelectionInterval(0, 0);
        }
        if (listSelectionListeners != null) {
            for (ListSelectionListener listSelectionListener : listSelectionListeners) {
                getCheckBoxListSelectionModel().addListSelectionListener(listSelectionListener);
            }
            listSelectionListeners = null;
        }
        setValueIsAdjusting(false);
        revalidate();
        repaint();
    }

    void unCheck() {
        getSelectionModel().clearSelection();
    }

    class MyChBoxListPopup extends JPopupMenu {

        private CheckBoxList list;
        private JMenuItem jmCheckAll;
        private JMenuItem jmCheckByRegex;
        private JMenuItem jmFindSelect;
        private JMenuItem jmUncheckAll;
        private JMenuItem jmUncheckByRegex;
        private JMenuItem jmShowAll;
        private JMenuItem jmShowChecked;
        private JMenuItem jmShowUnchecked;
        private JMenuItem jmHideChecked;
        private JMenuItem jmHideUnchecked;
        private JMenuItem jmShowStat;
        private JPanel w;
        private JMenuItem jmFind;
        private JMenuItem jmFindNext;
        private int lastIdx = -1;
        private JMenuItem jmFindPrev;
        private JMenuItem jmCopyValue;

        JDialog jd = null;
        JList jdList;

        JDialog jdStat = null;

        private MyChBoxListPopup(MyCheckBoxList _list, JPanel w) {
            InitItems();
            this.list = _list;
            this.w = w;
            addPopupMenuListener(new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    CheckBoxListSelectionModel checkBoxListSelectionModel = list.getCheckBoxListSelectionModel();
                    ListModel model = list.getModel();
                    boolean listEmpty = model.getSize() <= 0;
                    boolean allChecked = !listEmpty && !checkBoxListSelectionModel.isSelectedIndex(0);
                    boolean anyChecked = !listEmpty && isAnyChecked(checkBoxListSelectionModel, model);

                    jmCheckAll.setEnabled(allChecked);
                    jmCheckByRegex.setEnabled(allChecked);
                    jmFindSelect.setEnabled(allChecked);
                    jmUncheckAll.setEnabled(anyChecked);
                    jmUncheckByRegex.setEnabled(anyChecked);
                    jmShowStat.setEnabled(!listEmpty);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }

                private boolean isAnyChecked(CheckBoxListSelectionModel checkBoxListSelectionModel, ListModel model) {
                    for (int i = 0; i < model.getSize(); i++) {
                        Object elementAt = model.getElementAt(i);
                        if (elementAt.equals(CheckBoxList.ALL_ENTRY)) {
                            if (checkBoxListSelectionModel.isSelectedIndex(i)) {
                                return true;
                            }
                        } else {
                            if ((checkBoxListSelectionModel.isSelectedIndex(i))) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        public CheckBoxList getList() {
            return list;
        }

        private void SetChecked(boolean b) {
            CheckBoxListSelectionModel checkBoxListSelectionModel = list.getCheckBoxListSelectionModel();
            ListModel model = list.getModel();
            boolean listEmpty = model.getSize() <= 0;
            boolean AllEntryConfigured = !listEmpty && (model.getElementAt(0).equals(CheckBoxList.ALL_ENTRY));

            if (AllEntryConfigured) {
                if (b) {
                    checkBoxListSelectionModel.addSelectionInterval(0, 0);
                } else {
                    checkBoxListSelectionModel.removeSelectionInterval(0, 0);
                }
            } else {

            }
        }

        private void copyChecked(ActionEvent evt, boolean isChecked) {
            if (isChecked) {
                copyIndices(evt, list.getCheckBoxListSelectedIndices());
            } else {
                int len = list.getCheckBoxListSelectedIndices().length;
                if (len > 0) {
                    ListModel model = list.getModel();
                    CheckBoxListSelectionModel checkBoxListSelectionModel = list.getCheckBoxListSelectionModel();
                    ArrayList<Integer> buf = new ArrayList<>(model.getSize() - len);
                    for (int i = 0; i < model.getSize(); i++) {
                        if (!checkBoxListSelectionModel.isSelectedIndex(i)) {
                            buf.add(i);
                        }
                    }
                    int bufInt[] = new int[buf.size()];
                    for (int i = 0; i < buf.size(); i++) {
                        bufInt[i] = buf.get(i);

                    }
                    copyIndices(evt, bufInt);
                }
            }
        }

        private void copySelected(ActionEvent evt) {
            copyIndices(evt, list.getSelectedIndices());
        }

        private void copyIndices(ActionEvent evt, int[] selectedIndices) {
            ListModel model = list.getModel();
            if (selectedIndices != null && selectedIndices.length > 0) {
                StringBuilder buf = new StringBuilder(selectedIndices.length * 80);
                for (int selectedIndice : selectedIndices) {
                    Object elementAt = model.getElementAt(selectedIndice);
                    if (!elementAt.equals(CheckBoxList.ALL_ENTRY)) {
                        buf.append(model.getElementAt(selectedIndice)).append('\n');
                    }
                }
                SystemClipboard.copy(buf.toString());
            }

        }

        private void showStat(ActionEvent evt) {
            CheckBoxListSelectionModel checkBoxListSelectionModel = list.getCheckBoxListSelectionModel();
            ListModel model = list.getModel();

            int selCnt = 0;
            int total = model.getSize();
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).equals(CheckBoxList.ALL_ENTRY)) {
                    total--;
                } else if (checkBoxListSelectionModel.isSelectedIndex(i)) {
                    selCnt++;
                }
            }

            DefaultTableModel infoTableModel = new DefaultTableModel();
            infoTableModel.addColumn("");
            infoTableModel.addColumn("");
            infoTableModel.addRow(new Object[]{"Total elements", total});
            infoTableModel.addRow(new Object[]{"Selected", selCnt});
            infoTableModel.addRow(new Object[]{"Unselected", (total - selCnt)});
            infoTableModel.setRowCount(3);

            JTable tab = new JTable(infoTableModel);
            tab.getTableHeader().setVisible(false);
            tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            JPanel jp = new JPanel(new BorderLayout());
            jp.add(tab);

            inquirer.showInfoPanel(SwingUtilities.windowForComponent(jpo), "Statistic", jp, true);
        }

        private HashMap< Integer, String> getMatchedItems() {

            if (dlg == null) {
                dlg = new EnterRegexDialog(null, true);
            }
            dlg.setRegex(inquirer.geLocaltQuerySettings().getSavedFilters());
            dlg.setShowUps(false);
            ScreenInfo.setVisible((Window) SwingUtilities.getWindowAncestor(this), dlg, true);

            inquirer.logger.debug(dlg.getReturnStatus());
            if (dlg.getReturnStatus() == RET_OK) {
                HashMap<Integer, String> ret = new HashMap<>();
                lastRegEx = dlg.getSearch();
                inquirer.geLocaltQuerySettings().saveRegex(lastRegEx);

                ListModel model = list.getModel();

                for (int i = 0; i < model.getSize(); i++) {
                    Object elementAt = model.getElementAt(i);
                    if (!elementAt.equals(CheckBoxList.ALL_ENTRY) && dlg.checkMatch(elementAt.toString())) {
                        ret.put(i, elementAt.toString());
                    }
                }
                return ret;
            }
            return null;
        }

        private void findAndCheck(ActionEvent evt, boolean shouldCheck, ListSelectionModel theSelectionModel) {

            HashMap<Integer, String> matchedItems = getMatchedItems();
            if (matchedItems != null) {

                for (Integer i : matchedItems.keySet()) {
                    if (shouldCheck) {
                        theSelectionModel.addSelectionInterval(i, i);

                    } else {
                        theSelectionModel.removeSelectionInterval(i, i);
                    }

                }

                Window windowForComponent = SwingUtilities.windowForComponent(jpo);
                if (jd == null) {
                    if (windowForComponent instanceof JFrame) {
                        jd = new JDialog((JFrame) windowForComponent, true);
                    } else if (windowForComponent instanceof JDialog) {
                        jd = new JDialog((JDialog) windowForComponent, true);
                    }
                    JPanel jp = new JPanel();
                    jd.add(jp);
                    jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
                    jdList = new JList(new DefaultListModel());
                    jdList.setVisibleRowCount(20);
                    jp.add(new JScrollPane(jdList));

                    JButton btCl = new JButton("Close");
                    btCl.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            jd.dispose();
                        }
                    });
                    jp.add(btCl);
                    jd.setSize(320, 420);
                }
                jd.setTitle(((shouldCheck) ? "Checked" : "Unchecked") + " " + matchedItems.size() + " elements");
                DefaultListModel model1 = (DefaultListModel) jdList.getModel();
                model1.clear();
                for (String string : matchedItems.values()) {
                    model1.addElement(string);
                }
                ScreenInfo.CenterWindow(jd);
                jd.setVisible(true);

            }
        }

        private void InitItems() {
            jmCheckAll = new javax.swing.JMenuItem();
            jmCheckByRegex = new javax.swing.JMenuItem();
            jmFindSelect = new javax.swing.JMenuItem();
            jmUncheckAll = new javax.swing.JMenuItem();
            jmUncheckByRegex = new javax.swing.JMenuItem();
            jmShowAll = new javax.swing.JMenuItem();
            jmShowChecked = new javax.swing.JMenuItem();
            jmShowUnchecked = new javax.swing.JMenuItem();
            jmHideChecked = new javax.swing.JMenuItem();
            jmHideUnchecked = new javax.swing.JMenuItem();
            jmShowStat = new javax.swing.JMenuItem();
            jmFind = new javax.swing.JMenuItem();
            jmFindNext = new javax.swing.JMenuItem();
            jmFindPrev = new javax.swing.JMenuItem();

            jmCopyValue = new javax.swing.JMenuItem();
            jmCopyValue.setText("Copy selected (Ctrl-C)");
            jmCopyValue.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copySelected(evt);
                }

            });
            add(jmCopyValue);

            add(new JMenuItem(new AbstractAction("Copy checked") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyChecked(e, true);
                }
            }));

            add(new JMenuItem(new AbstractAction("Copy unchecked") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyChecked(e, false);
                }
            }));

            add(new javax.swing.JPopupMenu.Separator());

            jmFind.setText("Find (Ctrl-F)");
            jmFind.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    doSearch(true, true);
                }

            });
            add(jmFind);
            jmFindNext.setText("Find next (F3)");
            jmFindNext.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    doSearch(false, true);
                }

            });
            add(jmFindNext);
            jmFindPrev.setText("Find previous (Shift-F3)");
            jmFindPrev.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    doSearch(false, false);
                }

            });
            add(jmFindPrev);

            add(new javax.swing.JPopupMenu.Separator());

            jmCheckAll.setText("Check all");
            jmCheckAll.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    SetChecked(true);
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

            }
            );
            jmCheckAll.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
//                    jmCheckAllActionPerformed(evt);
                }
            });
            add(jmCheckAll);

            jmCheckByRegex.setText("Find and check");
            jmCheckByRegex.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    findAndCheck(evt, true, list.getCheckBoxListSelectionModel());
                }

            });
            add(jmCheckByRegex);

            jmFindSelect.setText("Find and select");
            jmFindSelect.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    findAndCheck(evt, true, list.getSelectionModel());
                }

            });
            add(jmFindSelect);

            add(new javax.swing.JPopupMenu.Separator());

            jmUncheckAll.setText("Uncheck all");
            jmUncheckAll.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    SetChecked(false);
                }
            });
            add(jmUncheckAll);

            jmUncheckByRegex.setText("Find and uncheck");
            add(jmUncheckByRegex);
            jmUncheckByRegex.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    findAndCheck(evt, false, list.getCheckBoxListSelectionModel());
                }

            });

            add(new javax.swing.JPopupMenu.Separator());
            jmShowStat.setText("Show stat");
            jmShowStat.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    showStat(evt);
                }
            });
            add(jmShowStat);

//            add(new javax.swing.JPopupMenu.Separator());
//            
//            jmShowAll.setText("Show all");
//            add(jmShowAll);
//            
//            jmShowChecked.setText("Show checked");
//            add(jmShowChecked);
//            
//            jmShowUnchecked.setText("Show unchecked");
//            add(jmShowUnchecked);
//            add(new javax.swing.JPopupMenu.Separator());
//            
//            jmHideChecked.setText("Hide checked");
//            add(jmHideChecked);
//            
//            jmHideUnchecked.setText("Hide unchecked");
//            add(jmHideUnchecked);
        }

    }

    class PopClickListener extends MouseAdapter {

        private final MyChBoxListPopup menu;

        private PopClickListener(MyChBoxListPopup menu) {
            this.menu = menu;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPop(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPop(e);
            }
        }

        private void doPop(MouseEvent e) {

            menu.show(e.getComponent(), e.getX(), e.getY());
        }

    }
}

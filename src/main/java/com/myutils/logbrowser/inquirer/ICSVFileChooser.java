package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.Main;
import org.immutables.value.Value;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


@Value.Immutable(singleton = true, builder = false)
@Main.MySingleton
public interface ICSVFileChooser {
    @Value.Lazy
    default JFileChooser dlg() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TXT & CSV Files", "txt", "csv");
        chooser.setFileFilter(filter);

        return chooser;
    }
}

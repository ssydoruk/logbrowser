package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.inquirer.EnterRegexDialog;
import org.immutables.value.Value;

@Value.Immutable(singleton = true, builder = false)
@Main.MySingleton
public interface IRegexDialog {
    @Value.Default
    default EnterRegexDialog findDlg () {
        return new EnterRegexDialog(null, true);
    }
}
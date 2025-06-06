package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.inquirer.EditRegexFields;
import org.immutables.value.Value;

@Value.Immutable(singleton = true, builder = false)
@Main.MySingleton
public interface IEditRegexFields {
    @Value.Lazy
    default EditRegexFields dlg() {
        return new EditRegexFields(null, true);
    }
}
